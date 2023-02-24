/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.gate;

import com.alibaba.fastjson.JSONObject;
import org.alan.mars.cluster.ClusterClient;
import org.alan.mars.cluster.ClusterMessage;
import org.alan.mars.cluster.ClusterSystem;
import org.alan.mars.curator.NodeType;
import org.alan.mars.message.*;
import org.alan.mars.net.Connect;
import org.alan.mars.net.ConnectListener;
import org.alan.mars.net.Inbox;
import org.alan.mars.netty.NettyConnect;
import org.alan.mars.protostuff.MessageUtil;
import org.alan.mars.protostuff.ProtostuffUtil;
import org.alan.mars.tips.GetServerStatus;
import org.alan.mars.tips.MarsResultEnum;
import org.alan.mars.constant.MessageConst.ToClientConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 玩家网关服务器会话对象
 * </p>
 * <p>
 * Created on 2017/3/27.
 *
 * @author Alan
 * @since 1.0
 */
@SuppressWarnings("unused")
public class GateSession extends NettyConnect implements Inbox<PFMessage>, ConnectListener {

    public static Map<String, GateSession> gateSessionMap = new HashMap<>();

    protected Logger log = LoggerFactory.getLogger(getClass());
    /**
     * 大厅通道
     */
    private ClusterClient currentClient;
    /**
     * 会话ID
     */
    public String sessionId;
    /**
     * 用户ID
     */
    public long userId;
    /**
     * 是否已认证
     */
    public boolean certify;

    public long activeTime;

    public long createTime;

    public Connect connect;

    public GateSession() {
    }

    /**
     * 收到集群中其他节点发送过来的消息
     */
    @Override
    public void onClusterReceive(Connect connect, PFMessage msg) {
        write(msg);
    }

    /**
     * 接收客户端发送过来的消息
     */
    @Override
    public void messageReceived(Object obj) {
        PFMessage msg = (PFMessage) obj;
        if (msg.messageType == ToClientConst.TYPE && msg.cmd == ToClientConst.REQ_GET_SERVER_STATUS) {
            checkServer();
            return;
        }
        //如果当前没有认证并且当前消息不是认证消息，断开连接
        if (!certify && msg.messageType != 1000) {
            log.warn("未认证状态，消息错误，message={}", msg);
            close();
            return;
        }
        if (msg.messageType == 1 && msg.cmd == 1) {//拦截ping消息
            activeTime = System.currentTimeMillis();
            PFMessage pfMessage = new PFMessage(1, 2, ProtostuffUtil.serialize(new PingPong.RespPong(activeTime)));
            write(pfMessage);
            return;
        }
        ClusterMessage clusterMessage = new ClusterMessage(sessionId, msg, userId);
        // 查询微服务消息
        try {
            Connect micconnect = ClusterSystem.system.microserviceAllot(currentClient, msg.messageType);
            if (micconnect != null) {
                micconnect.write(clusterMessage);
                return;
            }
        } catch (Exception e) {
            log.debug("微服务消息发送失败,messageType=" + msg.messageType, e);
        }
        if (connect == null || !connect.isActive()) {
            if (connect != null) {
                currentClient.close(connect);
            }
            connect = getConnect();
        }
        connect.write(clusterMessage);
    }

    /**
     * 用户连接关闭
     */
    @Override
    public void onClose() {
        log.debug("连接断开,sessionId={}", sessionId);
        gateSessionMap.remove(sessionId);
        sendClose();
        if (userId > 0) {
            // 移除session
            //向登录服务器发送用户下线
            sendLogout();
        }
        if (connect != null) {
            connect.removeConnectListener(this);
        }
        this.currentClient = null;
        this.connect = null;
    }

    public void sendLogout() {
        try {
            SessionLogout sessionLogout = new SessionLogout();
            sessionLogout.sessionId = sessionId;
            sessionLogout.userId = userId;
            PFMessage pfMessage = MessageUtil.getPFMessage(sessionLogout);
            ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage, userId);
            ClusterClient clusterClient = ClusterSystem.system.getByNodeType(NodeType.ACCOUNT, remoteAddress.getHost(), userId);
            if (currentClient != null) {
                clusterClient.getConnect().write(clusterMessage);
            }
        } catch (Exception e) {
            log.warn("用户下线消息发送异常", e);
        }
    }

    /**
     * 向当前连接节点发送关闭消息
     */
    private void sendClose() {
        if (connect == null || currentClient == null) {
            log.warn("向集群节点发送退出消息失败，连接为空");
            return;
        }
        SessionQuit sessionQuit = new SessionQuit();
        sessionQuit.sessionId = sessionId;
        PFMessage pfMessage = MessageUtil.getPFMessage(sessionQuit);
        ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage, userId);
        log.info(JSONObject.toJSONString(connect));
        connect.write(clusterMessage);
    }

    /**
     * 向当前连接节点发送进入消息
     */
    private void sendEnter() {
        if (connect == null || currentClient == null) {
            log.warn("向集群节点发送进入消息失败，连接为空");
            return;
        }
        log.debug("向集群节点发送进入消息,nodeName={},nodeAddress={}", currentClient.nodeConfig.getName(), currentClient.nodeConfig.getTcpAddress());
        SessionCreate sessionCreate = new SessionCreate();
        sessionCreate.sessionId = sessionId;
        sessionCreate.netAddress = remoteAddress;
        sessionCreate.userId = userId;
        sessionCreate.nodePath = ClusterSystem.system.getNodePath();
        PFMessage pfMessage = MessageUtil.getPFMessage(sessionCreate);
        ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage, userId);
        log.debug(JSONObject.toJSONString(connect));
        connect.write(clusterMessage);
    }

    /**
     * 用户连接建立
     */
    @Override
    public void onCreate() {
        sessionId = ctx.channel().id().asShortText();
        log.debug("连接成功，检查服务器状态，sessionId={},net={}", sessionId, remoteAddress);
        String host = remoteAddress.getHost();
        remoteAddress.setHost(host);
    }

    protected void checkServer() {
        log.debug("收到服务器检查消息，sessionId={},netAddress={}", sessionId, remoteAddress);
        activeTime = createTime = System.currentTimeMillis();
        // 将session添加到集群节点中
        gateSessionMap.put(sessionId, this);
        //连接建立成功以后，分配一个账号服务器进行账号认证
        currentClient = ClusterSystem.system.getByNodeType(NodeType.ACCOUNT, remoteAddress.getHost(), userId);
        sendServerStatus();
    }


    public void sendServerStatus() {
        if (currentClient == null) {
            log.debug("找不到可用的登录服务器");
            write(MessageUtil.getPFMessage(new GetServerStatus.RespGetServerStatus(MarsResultEnum.NETWORK_CANT_USE)));
            return;
        }
        connect = getConnect();
        if (connect == null) {
            log.debug("登录服务器连接不可用");
            write(MessageUtil.getPFMessage(new GetServerStatus.RespGetServerStatus(MarsResultEnum.NETWORK_CANT_USE)));
            return;
        }
        sendEnter();
        write(MessageUtil.getPFMessage(new GetServerStatus.RespGetServerStatus(MarsResultEnum.NETWORK_SUCCESS)));
    }

    public void switchNode(ClusterClient clusterClient) {
        log.debug("切换节点,sessionId={},userId={},srcPath={},targetPath={},certify={}", sessionId, userId, this.currentClient.nodeConfig.getName(), clusterClient.nodeConfig.getName(), certify);
        if (certify) {
            //向源节点发送退出
            sendClose();
            this.currentClient = clusterClient;
            this.connect = getConnect();
            sendEnter();
        }
    }

    public void onKickOut() {
        log.info("用户被顶号下线，sessionId={}，playerId={}", sessionId, userId);
        userId = 0;
        try {
            writeAndClose(MessageUtil.getPFMessage(new GetServerStatus.RespGetServerStatus(MarsResultEnum.PLAYER_KICK_OUT)));
        } catch (Exception e) {
            log.warn("用户被顶号下线,消息发送异常，", e);
        }
    }

    @Override
    public void onConnectClose(Connect connect) {
        log.warn("服务节点连接断开,userId={},sessionId={},nodeAddress={}", userId, sessionId, connect.address());
        try {
            writeAndClose(MessageUtil.getPFMessage(new GetServerStatus.RespGetServerStatus(MarsResultEnum.NETWORK_CANT_USE)));
        } catch (Exception e) {
            log.warn("服务节点连接断开,消息发送异常，", e);
        }
    }

    public Connect getConnect() {
        if (connect != null) {
            connect.removeConnectListener(this);
        }
        try {
            connect = currentClient.getConnectSync();
            connect.addConnectListener(this);
        } catch (Exception e) {
            log.warn("集群客户端获取连接异常,nodePath=" + currentClient.marsNode.getNodePath(), e);
            write(MessageUtil.getPFMessage(new GetServerStatus.RespGetServerStatus(MarsResultEnum.NETWORK_CANT_USE)));
            gateSessionMap.remove(sessionId);
            close();
        }
        return connect;
    }

    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - createTime > 5000 && !certify) {
            return false;
        }
        return currentTime - activeTime <= 30000;
    }

    public void setHost(String hostIp) {
        if (hostIp != null && !hostIp.isEmpty()) {
            remoteAddress = new NetAddress(hostIp, remoteAddress != null ? remoteAddress.getPort() : 0);
        }
    }

    @Override
    public String toString() {
        return "GateSession{" +
                "sessionId='" + sessionId + '\'' +
                ", userId=" + userId +
                ", certify=" + certify +
                ", connect=" + connect +
                '}';
    }
}
