package org.alan.mars.gate;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.alan.mars.protostuff.MessageUtil;
import org.alan.mars.message.PFMessage;

import java.util.List;

/**
 * Created on 2017/7/27.
 *
 * @author Alan
 * @since 1.0
 */
public class GateMessageEncoder extends MessageToMessageEncoder<PFMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, PFMessage msg, List<Object> out) throws Exception {
        out.add(MessageUtil.encode(msg));
    }
}
