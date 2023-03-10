package org.alan.mars.message;

import org.alan.mars.constant.MessageConst;
import org.alan.mars.protostuff.ProtobufMessage;

/**
 * Created on 2017/11/23.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.NOTIFY_SESSION_LOGOUT, resp = true, privately = true)
public class SessionLogout {
    public long userId;
    public String sessionId;
}
