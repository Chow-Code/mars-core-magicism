package org.alan.mars.message;

import org.alan.mars.constant.MessageConst;
import org.alan.mars.protostuff.ProtobufMessage;
import org.alan.mars.tips.KickOutType;

/**
 * Created on 2017/11/23.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.NOTIFY_SESSION_KICK_OUT, resp = true, privately = true)
public class SessionKickOut {
    public long userId;
    public String sessionId;
    public KickOutType reason;
}
