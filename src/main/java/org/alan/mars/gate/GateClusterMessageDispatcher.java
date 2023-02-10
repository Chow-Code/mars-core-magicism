/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.gate;

import io.netty.channel.ChannelHandler;
import org.alan.mars.cluster.ClusterMessage;
import org.alan.mars.cluster.ClusterMessageDispatcher;
import org.alan.mars.cluster.ClusterSystem;
import org.alan.mars.net.Connect;
import org.alan.mars.message.PFMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2017/4/6.
 *
 * @author Alan
 * @since 1.0
 */
@ChannelHandler.Sharable
public class GateClusterMessageDispatcher extends ClusterMessageDispatcher {

    private final Logger log = LoggerFactory.getLogger(getClass());

    public GateClusterMessageDispatcher(ClusterSystem clusterSystem) {
        super(clusterSystem);
    }

    public void onClusterReceive(Connect connect, ClusterMessage clusterMessage) {
        String sessionId = clusterMessage.sessionId;
        PFMessage pfMessage = clusterMessage.msg;
        if (sessionId != null && !sessionId.isEmpty()) {
            GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
            if (gateSession != null) {
                gateSession.onClusterReceive(connect, pfMessage);
            } else {
                log.warn("找不到sessionId={}的session，无法转发消息", sessionId);
            }
        } else {
            if (pfMessage != null && pfMessage.cmd == 2 && pfMessage.messageType == 1) {
                return;
            }
            super.handle(connect, null, pfMessage);
        }
    }
}
