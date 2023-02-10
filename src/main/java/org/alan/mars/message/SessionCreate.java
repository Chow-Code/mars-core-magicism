package org.alan.mars.message;

import org.alan.mars.constant.MessageConst.SessionConst;
import org.alan.mars.protostuff.ProtobufMessage;

/**
 * Created on 2017/10/21.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(messageType = SessionConst.TYPE, cmd = SessionConst.NOTIFY_SESSION_ENTER, resp = true, privately = true)
public class SessionCreate {
    public String sessionId;
    public NetAddress netAddress;
    public long userId;
    public String nodePath;
}
