package org.alan.mars.cluster;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.curator.*;
import org.alan.mars.gate.GateClusterMessageDispatcher;
import org.alan.mars.message.NetAddress;
import org.alan.mars.message.SwitchNodeMessage;
import org.alan.mars.net.Connect;
import org.alan.mars.netty.ConnectPool;
import org.alan.mars.netty.NettyServer;
import org.alan.mars.protostuff.PFSession;
import org.alan.mars.timer.TimerCenter;
import org.alan.mars.timer.TimerEvent;
import org.alan.mars.timer.TimerListener;
import org.alan.mars.utils.RandomUtil;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 集群系统总线
 * <p>
 * Created on 2017/3/28.
 *
 * @author Alan
 * @since 1.0
 */
@SuppressWarnings("unused")
@Component
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class ClusterSystem implements MarsNodeListener, ApplicationListener<ContextRefreshedEvent>, CommandLineRunner, TimerListener<String> {

    public static ClusterSystem system;
    /**
     * 集群各个节点客户端对象
     */
    private final Map<MarsNode, ClusterClient> clusterClientMap = new HashMap<>();
    /**
     * 玩家session集合
     */
    private final Map<String, PFSession> sessionMap = new ConcurrentHashMap<>();
    private ClusterMessageDispatcher clusterMessageDispatcher;
    /**
     * 连接通道初始化器
     */
    private ClusterConnectInitializer initializer;
    /**
     * 多线程连接通道初始化器
     */
    private ClusterConnectWorkPoolInitializer workPoolInitializer;
    /**
     * 微服务消息节点索引
     */
    private final Map<Integer, List<MarsNode>> microserviceIndexes = new HashMap<>();
    public final NodeManager nodeManager;
    public final MarsCurator marsCurator;
    public final NodeConfig nodeConfig;
    public final TimerCenter timerCenter;

    @Bean
    public ClusterMessageDispatcher clusterMessageDispatcher() {
        if (NodeType.GATE.toString().equals(nodeConfig.getType())) {
            return clusterMessageDispatcher = new GateClusterMessageDispatcher(this);
        } else {
            return clusterMessageDispatcher = new ClusterMessageDispatcher(this);
        }
    }

    @Bean
    public ClusterConnectInitializer createClusterConnectInitializer() {
        initializer = new ClusterConnectInitializer(clusterMessageDispatcher);
        workPoolInitializer = new ClusterConnectWorkPoolInitializer(clusterMessageDispatcher);
        return initializer;
    }

    public int sessionCount() {
        return sessionMap.size();
    }

    /**
     * 移除session
     */
    public PFSession removeSession(String sessionId) {
        return sessionMap.remove(sessionId);
    }

    /**
     * 增加session
     */
    public void addSession(PFSession pfSession){
        sessionMap.put(pfSession.getSessionId(), pfSession);
    }

    public PFSession getSession(String sessionId){
        return sessionMap.get(sessionId);
    }

    /**
     * 切换到固定节点
     */
    public void switchNode(PFSession pfSession, MarsNode marsNode) {
        log.info("[集群系统] 切换，sessionId={} -> {}", pfSession.getSessionId(), marsNode.getNodePath());
        try {
            SwitchNodeMessage switchNodeMessage = new SwitchNodeMessage(pfSession.getSessionId(), marsNode.getNodePath(), pfSession.getUserId());
            pfSession.send2Gate(switchNodeMessage);
            sessionMap.remove(pfSession.getSessionId());
        } catch (Exception e) {
            log.error("[集群系统] 切换异常", e);
        }
    }

    /**
     * 随机切换到指定类型的节点
     */
    public MarsNode switchNode(PFSession pfSession, NodeType nodeType) {
        try {
            MarsNode marsNode = randomMarsNode(nodeType, pfSession.getAddress().getHost(), pfSession.getUserId());
            switchNode(pfSession, marsNode);
            return marsNode;
        } catch (Exception e) {
            log.error("[集群系统] 切换异常", e);
        }
        return null;
    }

    /**
     * 随机切换到指定类型的节点
     */
    public MarsNode switchNode(PFSession pfSession, String nodePath) {
        try {
            MarsNode marsNode = getNode(nodePath);
            switchNode(pfSession, marsNode);
            return marsNode;
        } catch (Exception e) {
            log.error("[集群系统] 切换异常", e);
        }
        return null;
    }

    public String getNodePath() {
        return nodeManager.getMarNodePath(nodeConfig.getType(), nodeConfig.getName());
    }

    public ClusterClient randomOneByType(NodeType nodeType, String ip, long id) {
        MarsNode randomOneMarsNode = randomMarsNode(nodeType, ip, id);
        if (randomOneMarsNode == null) {
            return null;
        }
        return clusterClientMap.get(randomOneMarsNode);
    }

    public MarsNode randomMarsNode(NodeType nodeType, String ip, long id) {
        MarsNode marsNode = nodeManager.getMarNode(nodeType);
        if (marsNode == null || !marsNode.hasChildren()) {
            log.warn("[集群系统] 无法随机节点, 因为找不到该类型的节点. {}", nodeType);
            return null;
        }
        return marsNode.randomOneMarsNodeWithWeight(ip, id);
    }

    /**
     * 获取所有网关节点
     */
    public List<ClusterClient> getAllGate() {
        return getAllByType(NodeType.GATE);
    }

    /**
     * 随机一个网关节点
     *
     * @param clientIp 客户端IP 用于白名单验证
     */
    public MarsNode randomGate(long userId, String clientIp) {
        List<ClusterClient> gates = getAllGate();
        if (gates.isEmpty()) {
            return null;
        }
        List<MarsNode> collect = gates.stream().map(cluster -> cluster.marsNode).collect(Collectors.toList());
        List<MarsNode> marsNodes = MarsNode.canInNodes(collect, clientIp, userId);
        if (marsNodes == null){
            return null;
        }
        int[] weight = marsNodes.stream().mapToInt(c -> {
            //如果userId 为 -1 则不做黑白名单验证
            if (userId == -1) {
                return c.getNodeConfig().weight;
            }
            if (MarsNode.inWhiteList(nodeConfig,clientIp,userId)){
                return c.getNodeConfig().weight;
            }
            return c.getNodeConfig().weight;
        }).toArray();
        int index = RandomUtil.randomWidget(weight);
        if (index == -1) {
            return null;
        }
        return marsNodes.get(index);
    }

    /**
     * 随机一个网关节点
     */
    public MarsNode randomGate() {
        return randomGate(-1, null);
    }

    public List<ClusterClient> getAll() {
        return new ArrayList<>(clusterClientMap.values());
    }

    public List<ClusterClient> getAllByType(NodeType nodeType) {
        return clusterClientMap.values().stream().filter(c -> StrUtil.equals(nodeType.name(), c.getType())).collect(Collectors.toList());
    }

    public ClusterClient getByName(String name) {
        if (StrUtil.isBlank(name)){
            return null;
        }
        return clusterClientMap.values().stream().filter(clusterClient -> name.equals(clusterClient.nodeConfig.getName())).findAny().orElse(null);
    }

    public MarsNode getNode(String path) {
        if (path == null) {
            log.warn("[集群系统] 节点查找异常，path为空");
            return null;
        }
        return marsCurator.getMarsNode(path);
    }

    public ClusterClient getClusterByPath(String path) {
        MarsNode marsNode = marsCurator.getMarsNode(path);
        if (marsNode == null) {
            log.warn("[集群系统] 节点查找异常, 找不到该节点, path = {}.", path);
            return null;
        }
        return clusterClientMap.get(marsNode);
    }

    public ConnectPool getMarsConnectPool(NetAddress netAddress) {
        return new ConnectPool(netAddress, initializer, nodeConfig.clusterConnectPoolSize).init().start(timerCenter);
    }

    public void startClusterServer() {
        NetAddress netAddress = nodeConfig.getTcpAddress();
        NettyServer nettyServer = new NettyServer(netAddress.getPort(), nodeConfig.workPool ? workPoolInitializer : initializer);
        nettyServer.setName("tcp-nio-" + netAddress.getPort());
        nettyServer.start();
    }

    private void nodeAdd(MarsNode marsNode) {
        ClusterClient clusterClient = clusterClientMap.get(marsNode);
        if (clusterClient != null) {
            clusterClient.shutdown();
        }
        clusterClientMap.put(marsNode, new ClusterClient(marsNode, this));
        //增加微服务索引
        Set<Integer> micMessageTypes = marsNode.getNodeConfig().getMicServiceMessageTypes();
        if (micMessageTypes != null && !micMessageTypes.isEmpty()) {
            micMessageTypes.forEach(messageType -> {
                List<MarsNode> marsNodes = microserviceIndexes.computeIfAbsent(messageType, m -> new ArrayList<>());
                if (!marsNodes.contains(marsNode)) {
                    marsNodes.add(marsNode);
                }
            });
        }
    }

    /**
     * 当节点被移除时
     */
    private void nodeRemove(MarsNode marsNode) {
        ClusterClient clusterClient = clusterClientMap.remove(marsNode);
        if (clusterClient != null) {
            clusterClient.shutdown();
        }
        //删除微服务索引
        Set<Integer> micMessageTypes = marsNode.getNodeConfig().getMicServiceMessageTypes();
        if (micMessageTypes != null && !micMessageTypes.isEmpty()) {
            micMessageTypes.forEach(messageType -> {
                List<MarsNode> marsNodes = microserviceIndexes.get(messageType);
                if (marsNodes != null && marsNodes.contains(marsNode)) {
                    marsNodes.remove(marsNode);
                    if (marsNodes.isEmpty()) {
                        microserviceIndexes.remove(messageType);
                    }
                }
            });
        }
    }

    /**
     * 微服务消息分配
     */
    public Connect microserviceAllot(ClusterClient currentClient, int messageType) throws InterruptedException {
        List<MarsNode> microserviceNodes = microserviceIndexes.get(messageType);
        if (microserviceNodes == null || microserviceNodes.isEmpty()) {
            return null;
        }
        //如果当前用户所在连接，可以处理该微服务消息，则不选择其他连接
        if (microserviceNodes.contains(currentClient.marsNode)) {
            return null;
        }
        int index = (int) (Math.random() * microserviceNodes.size());
        MarsNode marsNode = microserviceNodes.get(index);
        ClusterClient clusterClient = clusterClientMap.get(marsNode);
        if (clusterClient != null) {
            return clusterClient.getConnect();
        }
        return null;
    }

    @Override
    public void nodeChange(NodeChangeType nodeChangeType, MarsNode marsNode) {
        log.debug("集群节点信息修改,nodePath={}", marsNode.getNodePath());
        switch (nodeChangeType) {
            case NODE_ADD:
                nodeAdd(marsNode);
                break;
            case NODE_REMOVE:
                nodeRemove(marsNode);
                break;
        }
    }

    @Override
    public void run(String... args) {
        system = this;
        startClusterServer();
        marsCurator.addMarsNodeListener(this);
        if (timerCenter != null) {
            timerCenter.add(new TimerEvent<>(this, "ClusterSystem", 1).setInitTime(10).withTimeUnit(TimeUnit.MINUTES));
        }
    }

    private static final AtomicBoolean created = new AtomicBoolean(false);

    private void writePidFile() {
        if (created.compareAndSet(false, true)) {
            try {
                File pidFile = new File("PID");
                log.info("[节点系统] 输出 PID 文件, file=" + pidFile.getAbsolutePath());
                new ApplicationPid().write(pidFile);
                pidFile.deleteOnExit();
            } catch (Exception ex) {
                String message = String.format("Cannot create pid file %s", "PID");
                log.warn("[节点系统] 不能输出PID 文件", ex);
            }
        }
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        writePidFile();
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        if ("ClusterSystem".equals(e.getParameter())) {
            log.info("[节点系统] 节点权重 = {}, session数 = {}", nodeConfig.weight, sessionMap.size());
            if (nodeConfig.weight == 0 && sessionMap.isEmpty()) {
                System.exit(0);
            }
        }
    }
}
