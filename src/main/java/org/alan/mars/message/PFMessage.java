package org.alan.mars.message;

import org.alan.mars.protostuff.ProtobufMessage;

/**
 * 消息基础类定义
 * Created on 2017/7/27.
 *
 * @author Alan
 * @since 1.0
 */
@ProtobufMessage(privately = true)
public class PFMessage {
    /* 消息类型 */
    public int messageType;
    /* 子命令字*/
    public int cmd;
    /* 请求ID */
    public int reqId;
    /* 数据*/
    public byte[] data;

    public PFMessage() {
    }

    public PFMessage(int messageType, int cmd, int reqId ,byte[] data) {
        this.messageType = messageType;
        this.cmd = cmd;
        this.reqId = reqId;
        this.data = data;
    }

    @Override
    public String toString() {
        return "PFMessage{" +
                "messageType=" + messageType +
                ", cmd=" + cmd +
                '}';
    }
}
