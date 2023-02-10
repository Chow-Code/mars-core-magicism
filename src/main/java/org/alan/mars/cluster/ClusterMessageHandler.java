/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.cluster;

import lombok.extern.slf4j.Slf4j;
import org.alan.mars.constant.MessageConst;
import org.alan.mars.gate.GateSession;
import org.alan.mars.listener.SessionCloseListener;
import org.alan.mars.listener.SessionEnterListener;
import org.alan.mars.listener.SessionLogoutListener;
import org.alan.mars.listener.SessionVerifyListener;
import org.alan.mars.message.*;
import org.alan.mars.net.Connect;
import org.alan.mars.netty.NettyConnect;
import org.alan.mars.protostuff.Command;
import org.alan.mars.protostuff.MessageType;
import org.alan.mars.protostuff.PFSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 集群消息处理器
 * <p>
 * Created on 2017/4/6.
 *
 * @author Alan
 * @since 1.0
 */
@Component
@MessageType(MessageConst.SessionConst.TYPE)
@Slf4j
public class ClusterMessageHandler {
    @Autowired
    private ClusterSystem clusterSystem;
    public SessionVerifyListener sessionVerifyListener;
    public SessionEnterListener sessionEnterListener;
    public SessionCloseListener sessionCloseListener;
    public SessionLogoutListener sessionLogoutListener;
    /**
     * 连接断开退出
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_QUIT)
    public void sessionClose(SessionQuit sessionQuit) {
        String sessionId = sessionQuit.sessionId;
        log.info("用户连接退出，sessionId={}", sessionId);
        PFSession pfSession = clusterSystem.sessionMap().remove(sessionId);
        if (sessionCloseListener != null && pfSession != null) {
            sessionCloseListener.sessionClose(pfSession);
        }
    }

    /**
     * 认证成功后通知给网关服务器
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_VERIFY_PASS)
    public void sessionVerifyPass(SessionVerifyPass sessionVerifyPass) {
        if (sessionVerifyListener != null) {
            sessionVerifyListener.userVerifyPass(sessionVerifyPass.sessionId, sessionVerifyPass.userId, sessionVerifyPass.ip);
        }
    }

    /**
     * 收到session进入消息
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_ENTER)
    public void sessionEnter(PFSession pfSession, Connect connect, SessionCreate sessionCreate) {
        String sessionId = sessionCreate.sessionId;
        long userId = sessionCreate.userId;
        String gatePath = sessionCreate.nodePath;
        log.info("用户连接进入，sessionId={}", sessionId);
        if (pfSession == null) {
            pfSession = new PFSession(sessionId, connect, sessionCreate.netAddress);
        }
        pfSession.setAddress(sessionCreate.netAddress);
        clusterSystem.sessionMap().put(sessionId, pfSession);
        pfSession.gatePath = gatePath;
        if (sessionEnterListener != null) {
            sessionEnterListener.sessionEnter(pfSession, userId);
        }
    }

    /**
     * session下线
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_LOGOUT)
    public void sessionLogout(Connect connect, SessionLogout sessionLogout) {
        String sessionId = sessionLogout.sessionId;
        long playerId = sessionLogout.playerId;
        log.info("用户下线，sessionId={}，playerId={}", sessionId, playerId);
        if (sessionLogoutListener != null) {
            sessionLogoutListener.logout(playerId, sessionId);
        }
    }

    /**
     * 踢出用户下线
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_KICK_OUT)
    public void sessionKickOut(Connect connect, SessionKickOut sessionKickout) {
        String sessionId = sessionKickout.sessionId;
        long playerId = sessionKickout.playerId;
        log.info("用户被顶号下线，sessionId={}，playerId={}", sessionId, playerId);
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null) {
            gateSession.onKickOut();
        }
    }

    @Command(MessageConst.SessionConst.CLUSTER_CONNECT_REGISTER)
    public void clusterRegister(NettyConnect connect, ClusterRegisterMsg clusterRegisterMsg) {
        if (clusterRegisterMsg == null) {
            log.debug("节点注册异常,connect={}", connect);
            return;
        }
        String nodePath = clusterRegisterMsg.nodePath;
        ClusterClient clusterClient = clusterSystem.getClusterByPath(nodePath);
        if (clusterClient != null) {
            clusterClient.connectPool.addConnect(connect);
            log.debug("节点注册成功,nodePath={},connect={}", nodePath, connect);
        }
    }


    @Command(MessageConst.SessionConst.NOTIFY_SWITCH_NODE)
    public void switchNode(SwitchNodeMessage switchNodeMessage) {
        String targetNodePath = switchNodeMessage.targetNodePath;
        String sessionId = switchNodeMessage.sessionId;
        long userId = switchNodeMessage.userId;
        ClusterClient clusterClient = clusterSystem.getClusterByPath(targetNodePath);
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null && clusterClient != null) {
            gateSession.switchNode(clusterClient);
        } else {
            log.warn("找不到gate session，sessionId={},gateSession={}", sessionId, gateSession);
        }
    }

    /**
     * 该消息需要广播给所有用户
     */
    @Command(MessageConst.SessionConst.BROADCAST_MSG)
    public void broadcast(BroadCastMessage broadCastMessage) {
        if (broadCastMessage.playerIds == null) {
            GateSession.gateSessionMap.values().forEach(session -> {
                //广播给已经认证的用户
                if (session != null && session.isActive() && session.certify) {
                    session.write(broadCastMessage.msg);
                    log.debug("广播完成 , userId = {} , msg = {}", session.userId, broadCastMessage.msg.getClass().getSimpleName());
                }
            });
        } else {
            GateSession.gateSessionMap.values().forEach(session -> {
                //广播给已经认证的用户且该用户在列表中
                if (session != null && session.isActive() && session.certify && broadCastMessage.playerIds.contains(session.userId)) {
                    session.write(broadCastMessage.msg);
                    log.debug("选择广播完成 , userId = {} , msg = {}", session.userId, broadCastMessage.msg.getClass().getSimpleName());
                }
            });
        }

    }

}
