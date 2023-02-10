package org.alan.mars.message;

import org.alan.mars.constant.MessageConst;
import org.alan.mars.protostuff.ProtobufMessage;

/**
 * Created on 2017/10/25.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(resp = true, messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.NOTIFY_SWITCH_NODE, privately = true)
public class SwitchNodeMessage {

    public String sessionId;
    //目标节点路径
    public String targetNodePath;

    public long userId;

    public SwitchNodeMessage(String sessionId, String targetNodePath, long userId) {
        this.sessionId = sessionId;
        this.targetNodePath = targetNodePath;
        this.userId = userId;
    }
}
