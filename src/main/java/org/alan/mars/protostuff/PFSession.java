package org.alan.mars.protostuff;

import lombok.Getter;
import lombok.Setter;
import org.alan.mars.cluster.ClusterMessage;
import org.alan.mars.message.PFMessage;
import org.alan.mars.message.SessionVerifyPass;
import org.alan.mars.net.Connect;
import org.alan.mars.message.NetAddress;
import org.alan.mars.net.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class PFSession extends Session{

    public static Logger log = LoggerFactory.getLogger(PFSession.class);

    public long userId;
    /* 网关节点PATH*/
    public String gatePath;
    /* 业务ID，用于根据该ID分配业务线程*/
    public int workId;

    public long activeTime;

    public PFSession(String sessionId, Connect connect, NetAddress address) {
        super(sessionId, connect, address);
        activeTime = System.currentTimeMillis();
    }

    @Override
    public void send(Object msg) {
        PFMessage pfMessage;
        if (msg instanceof PFMessage) {
            pfMessage = (PFMessage) msg;
        } else {
            pfMessage = MessageUtil.getPFMessage(msg);
        }
        if (pfMessage != null) {
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
    public void verifyPass(long userId, String ip, Object reference) {
        this.reference = reference;
        this.userId = userId;
        SessionVerifyPass sessionVerifyPass = new SessionVerifyPass();
        sessionVerifyPass.userId = userId;
        sessionVerifyPass.sessionId = sessionId;
        sessionVerifyPass.ip = ip;
        sessionVerifyPass.create = System.currentTimeMillis();
        PFMessage pfMessage = MessageUtil.getPFMessage(sessionVerifyPass);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        connect.write(clusterMessage);
    }

    public void onClose() {
        sessionListener.onSessionClose(this);
    }
}
