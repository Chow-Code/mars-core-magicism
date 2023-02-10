package org.alan.mars.gate;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.alan.mars.message.PFMessage;

import java.util.List;

/**
 * Created on 2017/7/27.
 *
 * @author Alan
 * @since 1.0
 */
public class GateMessageDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int messageType = msg.readUnsignedShort();
        int cmd = msg.readUnsignedShort();
        byte[] array = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), array, 0, array.length);
        PFMessage message = new PFMessage(messageType, cmd, array);
        out.add(message);
    }
}
