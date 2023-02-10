package org.alan.mars.message;

import org.alan.mars.constant.MessageConst.SessionConst;
import org.alan.mars.protostuff.ProtobufMessage;

/**
 * Created on 2017/10/21.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = SessionConst.TYPE, cmd = SessionConst.NOTIFY_SESSION_VERIFY_PASS, privately = true)
public class SessionVerifyPass {
    public String sessionId;
    public long userId;
    public long create;
    public String ip;
}
