package org.alan.mars.cluster;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.message.ClusterRegisterMsg;
import org.alan.mars.message.PFMessage;
import org.alan.mars.netty.NettyConnect;
import org.alan.mars.protostuff.MessageUtil;

/**
 * Created on 2017/3/27.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class ClusterConnect extends NettyConnect {

    private final ClusterMessageDispatcher clusterMessageDispatcher;
    private final ClusterMessage clusterMessage = new ClusterMessage(new PFMessage(1, 1, 0, null));

    public ClusterConnect(ClusterMessageDispatcher clusterMessageDispatcher) {
        this.clusterMessageDispatcher = clusterMessageDispatcher;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            //长时间未收到消息
            if (e.state() == IdleState.READER_IDLE) {
                log.debug("读取消息闲置,ctx={}", ctx);
                //ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {//长时间未写出消息
                log.debug("写消息闲置,ctx={}", ctx);
                write(clusterMessage);
                // ctx.close();
            } else {
                log.debug("连接空闲时间到,ctx={}", ctx);
                ctx.close();
            }
        }
    }

    @Override
    public void messageReceived(Object obj) {
        ClusterMessage msg = (ClusterMessage) obj;
        if (msg.msg.messageType != 1 || msg.msg.cmd != 2) {
            clusterMessageDispatcher.onClusterReceive(this, msg);
        }
    }

    @Override
    public void onClose() {
        //TODO
    }

    @Override
    public void onCreate() {
        ClusterRegisterMsg clusterRegisterMsg = new ClusterRegisterMsg();
        clusterRegisterMsg.nodePath = ClusterSystem.system.nodeManager.nodePath;
        if (clusterRegisterMsg.nodePath != null) {
            PFMessage pfMessage = MessageUtil.getPFMessage(clusterRegisterMsg);
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            write(clusterMessage);
        }
    }
}
