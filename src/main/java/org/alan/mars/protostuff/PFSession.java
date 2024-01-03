package org.alan.mars.protostuff;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.cluster.ClusterMessage;
import org.alan.mars.executor.TaskGroup;
import org.alan.mars.message.NetAddress;
import org.alan.mars.message.PFMessage;
import org.alan.mars.message.SessionVerifyPass;
import org.alan.mars.net.Connect;
import org.alan.mars.net.Session;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户session
 * <p>
 * Created on 2017/8/4.
 *
 * @author Alan
 * @since 1.0
 */
@Getter
@Setter
@Slf4j
public class PFSession extends Session {
    public final static AtomicInteger WORK_ID = new AtomicInteger();

    private long userId;
    private long playerId;
    /* 网关节点PATH*/
    private String gatePath;
    /* 业务ID，用于根据该ID分配业务线程*/
    private int workId;
    private long activeTime;
    /* 任务组*/
    private TaskGroup taskGroup;

    public PFSession(String sessionId, Connect connect, NetAddress address) {
        super(sessionId, connect, address);
        activeTime = System.currentTimeMillis();
        this.workId = WORK_ID.incrementAndGet();
        if (workId > 1000000) {
            WORK_ID.set(1);
        }
    }

    @Override
    public void send(Object msg) {
        send(msg, 0);
    }

    public void send(Object msg, int reqId) {
        PFMessage pfMessage;
        if (msg instanceof PFMessage) {
            pfMessage = (PFMessage) msg;
        } else {
            pfMessage = MessageUtil.getPFMessage(msg);
        }
        if (pfMessage != null) {
            pfMessage.reqId = reqId;
            ClusterMessage clusterMessage = new ClusterMessage(sessionId, pfMessage);
            connect.write(clusterMessage);
        }
    }

    public void send2Gate(Object msg) {
        PFMessage pfMessage = MessageUtil.getPFMessage(msg);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        connect.write(clusterMessage);
    }

    /**
     * 当用户验证通过后调用
     */
    public void verifyPass(long userId, int serverArea, long player, String ip, Object reference) {
        this.reference = reference;
        this.userId = userId;
        SessionVerifyPass sessionVerifyPass = new SessionVerifyPass();
        sessionVerifyPass.userId = userId;
        sessionVerifyPass.sessionId = sessionId;
        sessionVerifyPass.ip = ip;
        sessionVerifyPass.create = System.currentTimeMillis();
        sessionVerifyPass.serverArea = serverArea;
        sessionVerifyPass.playerId = player;
        PFMessage pfMessage = MessageUtil.getPFMessage(sessionVerifyPass);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        connect.write(clusterMessage);
    }

    public void onClose() {
        sessionListener.onSessionClose(this);
    }
}
