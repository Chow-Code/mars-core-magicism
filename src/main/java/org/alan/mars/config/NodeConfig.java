package org.alan.mars.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.alan.mars.message.NetAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 集群节点配置信息
 * <p>
 * Created on 2017/3/6.
 *
 * @author Alan
 * @since 1.0
 */
@ConfigurationProperties(prefix = "cluster")
@Component
@Getter
@Setter
@Order(1)
@ToString
public class NodeConfig {
    /**
     * 节点默认父目录
     */
    protected String parentPath = "cluster";
    /**
     * 节点类型
     */
    protected String type;
    /**
     * 节点名称
     */
    protected String name;
    /**
     * 标号
     */
    protected int num;
    /**
     * 节点tcp服务地址
     */
    protected NetAddress tcpAddress;
    /**
     * 节点http服务地址
     */
    protected NetAddress httpAddress;
    /**
     * 该节点接收的消息类型
     */
    public int[] messageTypes;
    /**
     * 该节点支持的游戏类型
     */
    public int[] gameTypes;
    /**
     * 节点权重
     */
    public int weight = 1;
    /**
     * IP白名单
     */
    public String[] whiteIpList;
    /**
     * 用户ID白名单
     */
    public long[] whiteIdList;
    /**
     * 单连接是否使用工作线程池
     */
    public boolean workPool;
    /**
     * 业务线程数
     */
    public int workPoolNum = Runtime.getRuntime().availableProcessors() * 16;
    /**
     * 节点支持的微服务消息号
     */
    public Set<Integer> micServiceMessageTypes;
    /**
     * 是否暴露微服务
     */
    public boolean showMicService = true;
    /**
     * 集群间连接数量
     */
    public int clusterConnectPoolSize = 6;
    /**
     * gate的公网IP
     */
    public String publicIp;
    /**
     * 使用代理的形式获取玩家的地址
     */
    public boolean useHaProxyAddress;
    /**
     * 是否支持会话转移
     */
    public boolean transferSession;
}
