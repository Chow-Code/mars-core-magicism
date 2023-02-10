package org.alan.mars.message;

import org.alan.mars.protostuff.ProtobufMessage;

/**
 * @author Zhoulindong on 16:04
 */
public class PingPong {

    @ProtobufMessage(messageType = 1, cmd = 1, desc = "Ping消息")
    public static class ReqPing {
        public long times;
    }

    @ProtobufMessage(resp = true, messageType = 1, cmd = 2, desc = "Pong消息")
    public static class RespPong {
        long time;

        public RespPong(long time) {
            this.time = time;
        }
    }

}
