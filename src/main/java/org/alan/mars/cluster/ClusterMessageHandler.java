package org.alan.mars.cluster;

import cn.hutool.core.collection.CollectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.MarsContext;
import org.alan.mars.constant.MessageConst;
import org.alan.mars.curator.NodeManager;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
@RequiredArgsConstructor
public class ClusterMessageHandler implements ApplicationRunner {
    private final ClusterSystem clusterSystem;
    private final NodeManager nodeManager;
    public List<SessionVerifyListener> verifyListeners = new ArrayList<>();
    public List<SessionEnterListener> enterListeners = new ArrayList<>();
    public List<SessionCloseListener> closeListeners = new ArrayList<>();
    public List<SessionLogoutListener> logoutListeners = new ArrayList<>();

    /**
     * 连接断开退出
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_QUIT)
    public void sessionClose(SessionQuit sessionQuit) {
        String sessionId = sessionQuit.sessionId;
        log.info("用户连接退出，sessionId = {}", sessionId);
        PFSession pfSession = clusterSystem.removeSession(sessionId);
        if (pfSession == null){
            return;
        }
        closeListeners.forEach(s -> s.sessionClose(pfSession));
    }

    /**
     * 认证成功后通知给网关服务器
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_VERIFY_PASS)
    public void sessionVerifyPass(SessionVerifyPass sessionVerifyPass) {
        verifyListeners.forEach(s -> s.userVerifyPass(sessionVerifyPass));
    }

    /**
     * 收到session进入消息
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_ENTER)
    public void sessionEnter(PFSession pfSession, Connect connect, SessionCreate sessionCreate) {
        String sessionId = sessionCreate.sessionId;
        long userId = sessionCreate.userId;
        long playerId = sessionCreate.playerId;
        String gatePath = sessionCreate.nodePath;
        log.info("用户进入，sessionId={}, userId = {}, playerId = {}", sessionId, userId, playerId);
        if (pfSession == null) {
            pfSession = new PFSession(sessionId, connect, sessionCreate.netAddress);
            pfSession.setWorkId((int) playerId);
        }
        pfSession.setSessionId(sessionId);
        pfSession.setAddress(sessionCreate.netAddress);
        clusterSystem.addSession(pfSession);
        pfSession.setGatePath(gatePath);
        PFSession finalPfSession = pfSession;
        enterListeners.forEach(s -> s.sessionEnter(finalPfSession, userId, playerId));
    }

    /**
     * session下线
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_LOGOUT)
    public void sessionLogout(Connect connect, SessionLogout sessionLogout) {
        String sessionId = sessionLogout.sessionId;
        long userId = sessionLogout.userId;
        log.info("用户下线，sessionId = {}，userId = {}, playerId = {}", sessionId, userId, sessionLogout.playerId);
        logoutListeners.forEach(s -> s.logout(sessionId, userId, sessionLogout.playerId));
    }

    /**
     * 踢出用户下线
     */
    @Command(MessageConst.SessionConst.NOTIFY_SESSION_KICK_OUT)
    public void sessionKickOut(Connect connect, SessionKickOut sessionKickout) {
        String sessionId = sessionKickout.sessionId;
        long userId = sessionKickout.userId;
        log.info("用户被顶号下线，sessionId = {}，userId = {}", sessionId, userId);
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null) {
            gateSession.onKickOut(sessionKickout.reason);
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
            log.info("节点注册成功, nodePath={}, connect={}", nodePath, connect);
        }
    }


    @Command(MessageConst.SessionConst.NOTIFY_SWITCH_NODE)
    public void switchNode(SwitchNodeMessage switchNodeMessage) {
        String targetNodePath = switchNodeMessage.targetNodePath;
        String sessionId = switchNodeMessage.sessionId;
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

        if (!CollectionUtil.isEmpty(broadCastMessage.serverAreas)) {
            List<Long> playerIds = new ArrayList<>();
            GateSession.gateSessionMap.values().forEach(session -> {
                //广播给已经认证的用户且该用户在列表中
                if (session != null && session.isActive() && session.certify && broadCastMessage.serverAreas.contains(session.serverArea)) {
                    session.write(broadCastMessage.msg);
                    playerIds.add(session.playerId);
                }
            });
            int length = playerIds.size();
            if (length > 20) {
                log.info("选择广播完成, 大区 = {}, 消息: {} - {}, 数量 = {}", broadCastMessage.serverAreas, broadCastMessage.msg.messageType, broadCastMessage.msg.cmd, length);
            } else {
                log.info("选择广播完成, 大区 = {}, 消息: {} - {}, 玩家 = {}", broadCastMessage.serverAreas, broadCastMessage.msg.messageType, broadCastMessage.msg.cmd, playerIds);
            }
            return;
        }
        if (!CollectionUtil.isEmpty(broadCastMessage.playerIds)) {
            GateSession.gateSessionMap.values().forEach(session -> {
                //广播给已经认证的用户且该用户在列表中
                if (session != null && session.isActive() && session.certify && broadCastMessage.playerIds.contains(session.playerId)) {
                    session.write(broadCastMessage.msg);
                }
            });
            int length = broadCastMessage.playerIds.size();
            if (length > 20) {
                log.info("选择广播完成, 大区 = {}, 消息: {} - {}, 数量 = {}", broadCastMessage.serverAreas, broadCastMessage.msg.messageType, broadCastMessage.msg.cmd, length);
            } else {
                log.info("选择广播完成, 大区 = {}, 消息: {} - {}, 玩家 = {}", broadCastMessage.serverAreas, broadCastMessage.msg.messageType, broadCastMessage.msg.cmd, broadCastMessage.playerIds);
            }
            return;
        }
        GateSession.gateSessionMap.values().forEach(session -> {
            //广播给已经认证的用户
            if (session != null && session.isActive() && session.certify) {
                session.write(broadCastMessage.msg);
            }
        });
        log.info("全服广播完成, 消息: {} - {}", broadCastMessage.msg.messageType, broadCastMessage.msg.cmd);
    }

    @Command(MessageConst.SessionConst.SAVE_NODE_CONFIG)
    public void saveNodeConfig(SaveNodeConfig config) {
        try {
            if (config.weight != null) {
                clusterSystem.nodeConfig.setWeight(Integer.parseInt(config.weight));
            }
            List<Long> idWhitelist = config.idWhitelist;
            List<String> ipWhiteList = config.addressWhitelist;
            if (idWhitelist != null && !idWhitelist.isEmpty()) {
                long[] whiteIdList = idWhitelist.stream().mapToLong(Long::longValue).toArray();
                clusterSystem.nodeConfig.setWhiteIdList(whiteIdList);
            } else {
                clusterSystem.nodeConfig.setWhiteIdList(new long[]{});
            }
            if (ipWhiteList != null && !ipWhiteList.isEmpty()) {
                String[] whiteIpList = ipWhiteList.toArray(ipWhiteList.toArray(new String[0]));
                clusterSystem.nodeConfig.setWhiteIpList(whiteIpList);
            } else {
                clusterSystem.nodeConfig.setWhiteIpList(new String[]{});
            }
            nodeManager.update();
        } catch (Exception e) {
            log.error("保存节点出错", e);
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        verifyListeners.addAll(MarsContext.getContext().getBeansOfType(SessionVerifyListener.class).values());
        closeListeners.addAll(MarsContext.getContext().getBeansOfType(SessionCloseListener.class).values());
        enterListeners.addAll(MarsContext.getContext().getBeansOfType(SessionEnterListener.class).values());
        logoutListeners.addAll(MarsContext.getContext().getBeansOfType(SessionLogoutListener.class).values());
    }
}
