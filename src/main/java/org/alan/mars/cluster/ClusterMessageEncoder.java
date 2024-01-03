package org.alan.mars.cluster;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.alan.mars.protostuff.ProtostuffUtil;

import java.util.List;

/**
 * Created on 2017/7/27.
 *
 * @author Alan
 * @since 1.0
 */
public class ClusterMessageEncoder extends MessageToMessageEncoder<ClusterMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ClusterMessage msg, List<Object> out) {
        out.add(Unpooled.wrappedBuffer(ProtostuffUtil.serialize(msg)));
    }
}
