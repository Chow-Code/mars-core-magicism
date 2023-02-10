/**
 * Copyright Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 * <p>
 * 2017年3月2日
 */
package org.alan.mars.curator;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.config.ZookeeperConfig;
import org.alan.mars.data.MarsConst;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
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
public class MarsCurator implements ApplicationListener<ContextRefreshedEvent>, ApplicationRunner, TreeCacheListener, DisposableBean {
    public static final String ALL = "ALL";

    private CuratorFramework client = null;

    @Autowired
    private ZookeeperConfig zkConfig;

    private String rootPath;

    public String nodePath;

    private MarsNode marsRootNode;

    private TreeCache treeCache;

    private Set<MarsCuratorListener> listeners = new CopyOnWriteArraySet<>();

    private final Map<String, Set<MarsNodeListener>> marsNodeListeners = new HashMap<>();

    public MarsCurator() {
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Map<String, MarsCuratorListener> tmpMap = context.getBeansOfType(MarsCuratorListener.class);
        if (!tmpMap.isEmpty()) {
            listeners = new CopyOnWriteArraySet<>(tmpMap.values());
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("mars curator 初始化. ");
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(zkConfig.getBaseSleepTimeMs(), zkConfig.getMaxRetries());
            client = CuratorFrameworkFactory.newClient(zkConfig.getConnects(), zkConfig.getSessionTimeoutMs(), zkConfig.getConnectionTimeoutMs(), retryPolicy);
            client.start();
            initRootPath();
            cacheMarsNode();
        } catch (Exception e) {
            log.warn("mars curator init fail...", e);
        }
    }

    public void stop() {
        try {
            if (treeCache != null) {
                treeCache.close();
            }
            if (client != null) {
                client.close();
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

    public CuratorFramework getClient() {
        return client;
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
            log.info("[节点控制] 通知到各节点监听器: path={},nodeChangeType={},listener={}", path, nodeChangeType, listener);
        });
        if ((nodePath == null || !nodePath.equals(path)) && !rootPath.equals(path) && marsNode.getNodeConfig() != null) {
            set = marsNodeListeners.computeIfAbsent(ALL, e -> new HashSet<>());
            set.forEach(listener -> {
                listener.nodeChange(nodeChangeType, marsNode);
                log.info("[节点控制] 通知到各节点监听器: path={},nodeChangeType={},listener={}", path, nodeChangeType, listener);
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

    private void cacheMarsNode() throws Exception {
        treeCache = new TreeCache(client, rootPath);
        treeCache.getListenable().addListener(this);
        treeCache.start();
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
    public void childEvent(CuratorFramework client, TreeCacheEvent event)
            throws Exception {
        TreeCacheEvent.Type type = event.getType();
        ChildData childData = event.getData();
        if (childData == null) {
            log.debug("No data in event[{}]", event);
            switch (type) {
                case CONNECTION_LOST:
                    if (client.getZookeeperClient().blockUntilConnectedOrTimedOut()) {
//            case CONNECTION_SUSPENDED:
                        restart();
                    }
                    break;
                case CONNECTION_RECONNECTED:
                case INITIALIZED:
                    notifyRefreshed();
                    break;
            }
        } else {
            byte[] byteData = childData.getData();
            if (byteData == null) {
                byteData = new byte[0];
            }
            String data = new String(byteData, StandardCharsets.UTF_8);
            String strStat = JSON.toJSONString(childData.getStat());
            String path = childData.getPath();
            log.debug("[节点控制] 收到节点事件: " + "type=[{}], path={}, data={}, stat={}", type, path, data, strStat);
            switch (type) {
                case NODE_ADDED:
                    addMarsNode(path, data, strStat);
                    break;
                case NODE_UPDATED:
                    updateMarsNode(path, data, strStat);
                    break;
                case NODE_REMOVED:
                    removeMarsNode(path);
                    break;
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("[节点控制] 监听到节点关闭");
        stop();
    }
}
