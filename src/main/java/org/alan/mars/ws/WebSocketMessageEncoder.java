package org.alan.mars.ws;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.alan.mars.protostuff.MessageUtil;
import org.alan.mars.message.PFMessage;

import java.util.List;

/**
 * Created on 2020/3/31.
 *
 * @author Alan
 * @since 1.0
 */
public class WebSocketMessageEncoder extends MessageToMessageEncoder<PFMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, PFMessage msg, List<Object> out) {
        out.add(new BinaryWebSocketFrame(MessageUtil.encode(msg)));
    }
}
