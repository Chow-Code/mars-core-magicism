package org.alan.mars.message;

import org.alan.mars.constant.MessageConst;
import org.alan.mars.protostuff.ProtobufMessage;

/**
 * Created on 2017/11/24.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(messageType = MessageConst.SessionConst.TYPE, cmd = MessageConst.SessionConst.CLUSTER_CONNECT_REGISTER,resp = true, privately = true)
public class ClusterRegisterMsg {
    public String nodePath;
}
