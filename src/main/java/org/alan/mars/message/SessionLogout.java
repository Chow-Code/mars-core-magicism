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
    public String sessionId;
    public long userId;
    public long playerId;

    public SessionLogout(String sessionId, long userId, long playerId) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.playerId = playerId;
    }
}
