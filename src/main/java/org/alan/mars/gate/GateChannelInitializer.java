package org.alan.mars.gate;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.MarsContext;
import org.alan.mars.cloudflare.HAProxyMessageHandler;
import org.alan.mars.config.NodeConfig;

/**
 * Created on 2017/4/6.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class GateChannelInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MSG_MAX_SIZE = 10 * 1024 * 1024;

    public static final int HEADER_SIZE = 4;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        NodeConfig config = MarsContext.getBean(NodeConfig.class);
        if (config.useHaProxyAddress){
            //添加 HAProxyMessageDecoder 以处理 PROXY 协议
            pipeline.addLast(new HAProxyMessageDecoder())
            // 添加自定义处理器以处理解码后的 HAProxyMessage
                    .addLast(new HAProxyMessageHandler());
        }
        pipeline.addLast(new LengthFieldBasedFrameDecoder(MSG_MAX_SIZE, 0, HEADER_SIZE, 0, 4))
                .addLast(new GateMessageDecoder())
                .addLast(new LengthFieldPrepender(HEADER_SIZE))
                .addLast(new GateMessageEncoder())
                .addLast(new GateSession());
    }
}
