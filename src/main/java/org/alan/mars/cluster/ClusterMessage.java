package org.alan.mars.cluster;

import org.alan.mars.message.PFMessage;

/**
 * Created on 2017/10/19.
 *
 * @author Alan
 * @since 1.0
 */
public class ClusterMessage {
    public String sessionId;
    public PFMessage msg;
    public long userId;

    public ClusterMessage(PFMessage msg) {
        this.msg = msg;
    }

    public ClusterMessage(String sessionId, PFMessage msg) {
        this.sessionId = sessionId;
        this.msg = msg;
    }

    public ClusterMessage(String sessionId, PFMessage msg, long userId) {
        this.sessionId = sessionId;
        this.msg = msg;
        this.userId = userId;
    }
}
