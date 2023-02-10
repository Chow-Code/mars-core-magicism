package org.alan.mars.message;

import org.alan.mars.protostuff.ProtobufMessage;
import org.alan.mars.constant.MessageConst;

import java.util.Set;

/**
 * Created on 2017/10/25.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.BROADCAST_MSG, privately = true)
public class BroadCastMessage {

    public PFMessage msg;

    public Set<Long> playerIds;

    public BroadCastMessage() {
    }

    public BroadCastMessage(PFMessage msg) {
        this.msg = msg;
    }
}
