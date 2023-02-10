package org.alan.mars.tips;

import org.alan.mars.constant.MessageConst;
import org.alan.mars.protostuff.ProtobufMessage;
import lombok.ToString;
import org.alan.mars.tips.MarsResultEnum;

/**
 * @author Zhoulindong on 11:29
 */
public class GetServerStatus {
    @ProtobufMessage(desc = "请求获取服务器状态",messageType = MessageConst.ToClientConst.TYPE, cmd = MessageConst.ToClientConst.REQ_GET_SERVER_STATUS)
    @ToString
    public static class ReqGetServerStatus {
        public byte non;
    }

    @ProtobufMessage(desc = "响应获取服务器状态",messageType = MessageConst.ToClientConst.TYPE, cmd = MessageConst.ToClientConst.RESP_GET_SERVER_STATUS, resp = true)
    @ToString
    public static class RespGetServerStatus {
        public MarsResultEnum marsResultEnum;

        public RespGetServerStatus(MarsResultEnum marsResultEnum) {
            this.marsResultEnum = marsResultEnum;
        }
    }
}