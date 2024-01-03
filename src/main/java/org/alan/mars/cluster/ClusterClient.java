package org.alan.mars.cluster;

import lombok.ToString;
import org.alan.mars.config.NodeConfig;
import org.alan.mars.curator.MarsNode;
import org.alan.mars.net.Connect;
import org.alan.mars.netty.ConnectPool;

/**
 * <p>
 * 集群客户端对象
 * </p>
 * <p>
 * Created on 2017/3/28.
 *
 * @author Alan
 * @since 1.0
 */
@ToString
public class ClusterClient {
    /**
     * 节点的配置信息
     */
    public NodeConfig nodeConfig;
    /**
     * 集群节点信息
     */
    public MarsNode marsNode;
    /**
     * 连接池
     */
    public ConnectPool connectPool;

    private final ClusterSystem clusterSystem;

    public ClusterClient(MarsNode marsNode, ClusterSystem clusterSystem) {
        this.clusterSystem = clusterSystem;
        init(marsNode);
    }

    public void init(MarsNode marsNode) {
        this.marsNode = marsNode;
        nodeConfig = marsNode.getNodeConfig();
        this.connectPool = clusterSystem.getMarsConnectPool(nodeConfig.getTcpAddress());
    }

    public Connect getConnect() {
        return connectPool.getConnect();
    }

    public Connect getConnectSync() throws InterruptedException {
        return connectPool.getConnectSync();
    }

    public void write(Object msg) {
        connectPool.getConnect().write(msg);
    }

    public boolean canReceive(int messageType) {
        if (marsNode != null && marsNode.getNodeConfig() != null && marsNode.getNodeConfig().getMicServiceMessageTypes() != null) {
            return marsNode.getNodeConfig().getMicServiceMessageTypes().contains(messageType);
        }
        return false;
    }

    public String getType() {
        return nodeConfig.getType();
    }

    public void close(Connect connect) {
        connectPool.close(connect);
        if (connect != null) {
            connect.close();
        }
    }

    public void shutdown() {
        if (connectPool != null) {
            connectPool.shutdown();
        }
    }

}
