package org.alan.mars.protostuff;

import org.alan.mars.cluster.ClusterMessage;
import org.alan.mars.message.PFMessage;
import org.alan.mars.message.PingPong;
import org.alan.mars.net.Connect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 心跳消息处理器
 * <p>
 * author Alan
 * date 2017/8/20
 */
@Component
@MessageType(1)
public class PingMessageHandler {

    public static final Logger log = LoggerFactory.getLogger(PingMessageHandler.class);


    @Command(1)
    public void ping(PFSession session, Connect connect) {
        if (session != null) {
            session.send(new PingPong.RespPong(System.currentTimeMillis()));
        } else if (connect != null) {
            PingPong.RespPong respPong = new PingPong.RespPong(0);
            PFMessage pfMessage = new PFMessage(1, 2, 0, ProtostuffUtil.serialize(respPong));
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            connect.write(clusterMessage);
        }
    }
}
