package org.alan.mars.curator;

import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.config.ZookeeperConfig;
import org.alan.mars.data.MarsConst;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * zookeeper 监视器管理类
 *
 * @author Alan
 */
@Order(3)
@Component
@Slf4j
@RequiredArgsConstructor
public class MarsCurator implements ApplicationListener<ContextRefreshedEvent>, ApplicationRunner, CuratorCacheListener, ConnectionStateListener, DisposableBean {
    public static final String ALL = "ALL";

    @Getter
    private CuratorFramework client = null;

    private final ZookeeperConfig zkConfig;

    private String rootPath;

    public String nodePath;

    private MarsNode marsRootNode;

    private CuratorCache curatorCache;

    private Set<MarsCuratorListener> listeners = new CopyOnWriteArraySet<>();

    private final Map<String, Set<MarsNodeListener>> marsNodeListeners = new HashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Map<String, MarsCuratorListener> tmpMap = context.getBeansOfType(MarsCuratorListener.class);
        if (!tmpMap.isEmpty()) {
            listeners = new CopyOnWriteArraySet<>(tmpMap.values());
        }
    }

    @Override
    public void run(ApplicationArguments args){
        try {
            log.info("mars curator 初始化. ");
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(zkConfig.getBaseSleepTimeMs(), zkConfig.getMaxRetries());
            client = CuratorFrameworkFactory.newClient(zkConfig.getConnects(), zkConfig.getSessionTimeoutMs(), zkConfig.getConnectionTimeoutMs(), retryPolicy);
            client.start();
            client.blockUntilConnected();
            initRootPath();
            cacheMarsNode();
        } catch (Exception e) {
            log.warn("Mars Curator 初始化失败", e);
        }
    }

    public void stop() {
        try {
            if (curatorCache != null) {
                log.debug("treeCache 关闭");
                curatorCache.close();
                log.debug("treeCache 已关闭");
            }
            if (client != null) {
                log.debug("client 关闭");
                client.close();
                log.debug("client 已关闭");
            }
        } catch (Exception e) {
            log.warn("mars curator stop fail.", e);
        }
    }

    public void restart() {
        try {
            stop();
            run(null);
        } catch (Exception e) {
            log.warn("restart fail.", e);
        }
    }

    /**
     * 锁服务，获得一个指定路径的锁
     */
    public InterProcessMutex getLock(String lockPath) {
        return new InterProcessMutex(client, lockPath);
    }

    public String addPath(String path, byte[] payload, boolean persistent) {
        nodePath = mkPath(path);
        try {
            if (!checkExists(nodePath)) {
                log.info("[节点控制] 创建节点 {}", nodePath);
                client.create()
                        .creatingParentsIfNeeded().withMode(persistent
                                ? CreateMode.PERSISTENT : CreateMode.EPHEMERAL)
                        .forPath(nodePath, payload);
            }
        } catch (Exception e) {
            log.warn("[节点控制] 增加节点失败 , path is {}", path, e);
        }
        return nodePath;
    }

    public String updatePath(String path, byte[] payload, boolean persistent) {
        nodePath = mkPath(path);
        try {
            if (checkExists(nodePath)) {
                log.info("update path {}", nodePath);
                client.setData().forPath(nodePath, payload);
            }
        } catch (Exception e) {
            log.warn("update path fail.path is {}", path, e);
        }
        return nodePath;
    }

    public void addMarsNodeListener(String path, MarsNodeListener listener) {
        log.info("[节点控制] 增加节点监听: path={},listener={}", path, listener);
        Set<MarsNodeListener> set = marsNodeListeners.computeIfAbsent(path, e -> new HashSet<>());
        set.add(listener);
    }

    public void removeMarsNodeListener(String path, MarsNodeListener listener) {
        log.info("[节点控制] 移除节点监听: path={},listener={}", path, listener);
        Set<MarsNodeListener> set = marsNodeListeners.computeIfAbsent(path, e -> new HashSet<>());
        set.remove(listener);
    }

    /**
     * 以根节点添加监听
     */
    public void addMarsNodeListener(MarsNodeListener listener) {
        log.info("[节点控制] 增加节点监听: path={},listener={}", ALL, listener);
        Set<MarsNodeListener> set = marsNodeListeners.computeIfAbsent(ALL, e -> new HashSet<>());
        set.add(listener);
    }

    private void notifyMarsNodeListener(MarsNodeListener.NodeChangeType nodeChangeType, MarsNode marsNode) {
        String path = marsNode.getNodePath();
        log.debug("[节点控制] 通知到各节点监听器: path={},nodeChangeType={}", path, nodeChangeType);
        Set<MarsNodeListener> set = marsNodeListeners.computeIfAbsent(path, e -> new HashSet<>());
        set.forEach(listener -> {
            listener.nodeChange(nodeChangeType, marsNode);
            log.info("[节点控制] 通知到各节点监听器: path={},nodeChangeType={},listener={}", path, nodeChangeType, listener.getClass().getSimpleName());
        });
        if ((nodePath == null || !nodePath.equals(path)) && !rootPath.equals(path) && marsNode.getNodeConfig() != null) {
            set = marsNodeListeners.computeIfAbsent(ALL, e -> new HashSet<>());
            set.forEach(listener -> {
                listener.nodeChange(nodeChangeType, marsNode);
                log.info("[节点控制] 通知到各节点监听器: path={},nodeChangeType={},listener={}", path, nodeChangeType, listener.getClass().getSimpleName());
            });
        }
    }


    public MarsNode getMarsNode(String path) {
        //path = mkPath(path);
        return marsRootNode.getChildren(path, true);
    }

    private void addMarsNode(String path, String data, String strStat) {
        MarsNode marsNode = new MarsNode(path, data, strStat);
        if (path.equals(rootPath)) {
            marsRootNode = marsNode;
        } else {
            marsRootNode.addChildren(marsNode);
        }
        notifyMarsNodeListener(MarsNodeListener.NodeChangeType.NODE_ADD, marsNode);
    }

    private void updateMarsNode(String path, String data, String strStat) {
        MarsNode marsNode = marsRootNode.getChildren(path, true);
        if (marsNode != null) {
            marsNode.updateData(data);
            marsNode.setStrStat(strStat);
        }
    }

    private void removeMarsNode(String path) {
        MarsNode marsNode = marsRootNode.removeChildren(path);
        if (marsNode != null) {
            notifyMarsNodeListener(MarsNodeListener.NodeChangeType.NODE_REMOVE, marsNode);
        } else {
            log.warn("[节点控制] 找不到指定的节点,path={}", path);
        }
    }

    private void cacheMarsNode(){
        curatorCache = CuratorCache.build(client, rootPath);
        curatorCache.listenable().addListener(this);
        curatorCache.start();
    }

    private boolean checkExists(String path) throws Exception {
        ExistsBuilder existsBuilder = client.checkExists();
        Stat stat = existsBuilder.forPath(path);
        return stat != null;
    }

    private String mkPath(String path) {
        return rootPath + path;
    }

    private void initRootPath() {
        try {
            rootPath = MarsConst.SEPARATOR + zkConfig.getMarsRoot();
            if (!checkExists(rootPath)) {
                log.error("根节点不存在 {}.", rootPath);
                client.create().forPath(rootPath);
            }
        } catch (Exception e) {
            log.error("[节点控制] 根节点初始化失败! ", e);
        }
    }

    private void notifyRefreshed() {
        listeners.forEach(listener -> {
            try {
                listener.marsCuratorRefreshed(this);
            } catch (Exception e) {
                log.warn("[节点控制] 通知节点刷新失败! ", e);
            }
        });
    }

    @Override
    public void destroy() {
        log.info("[节点控制] 监听到节点关闭");
        stop();
    }

    @Override
    public void initialized() {
        notifyRefreshed();
    }

    @SneakyThrows
    @Override
    public void event(Type type, ChildData oldData, ChildData childData) {
        ChildData child = null;
        if (oldData != null){
            child = oldData;
        }
        if (childData != null){
            child = childData;
        }
        if (child == null){
            log.info("[节点控制] 收到节点事件, 但无法处理, data都为空: " + "type={}", type);
            return;
        }
        byte[] byteData = child.getData();
        if (byteData == null) {
            byteData = new byte[0];
        }
        String data = new String(byteData, StandardCharsets.UTF_8);
        String strStat = JSONUtil.toJsonStr(child.getStat());
        String path = child.getPath();
        log.info("[节点控制] 收到节点事件: " + "type=[{}], path={}, info={}, stat={}", type, path, data, strStat);
        switch (type) {
            case NODE_CREATED:
                addMarsNode(path, data, strStat);
                break;
            case NODE_CHANGED:
                updateMarsNode(path, data, strStat);
                break;
            case NODE_DELETED:
                removeMarsNode(path);
                break;
        }
    }

    @SneakyThrows
    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        switch (newState){
            case RECONNECTED:
                restart();
                break;
            case LOST:
                if (client.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
                    restart();
                }
                break;
        }
    }
}
