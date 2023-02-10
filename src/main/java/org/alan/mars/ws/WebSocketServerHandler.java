package org.alan.mars.ws;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.message.PFMessage;
import org.alan.mars.gate.GateSession;

/**
 * Created on 2018/4/12.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {


    /**
     * 当客户端连接成功，返回个成功信息
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        GateSession gateSession = new WSGateSession();
        gateSession.channelActive(ctx);
        GateSession.gateSessionMap.put(gateSession.sessionId, gateSession);
    }
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    String sessionId = ctx.channel().id().asShortText();
                    GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
                    if (gateSession != null) {
                        log.warn("连接读闲置时间到，即将被关闭,activeTime={},ctx={}", gateSession.activeTime, ctx);
                    } else {
                        log.warn("连接读闲置时间到，即将被关闭,ctx={}", ctx);
                    }
                    ctx.close();
                    break;
                case ALL_IDLE:
                default:
                    break;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 当客户端断开连接
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String sessionId = ctx.channel().id().asShortText();
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null) {
            gateSession.channelInactive(ctx);
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    public String getRealIp(FullHttpRequest request) {
        if (request.headers().contains("X-Forwarded-For")) {
            return request.headers().get("X-Forwarded-For");
        } else if (request.headers().contains("x-forwarded-for")) {
            return request.headers().get("x-forwarded-for");
        } else if (request.headers().contains("HTTP_X_FORWARDED_FOR")) {
            return request.headers().get("HTTP_X_FORWARDED_FOR");
        } else if (request.headers().contains("X-Real_IP")) {
            return request.headers().get("X-Real_IP");
        } else
            return request.headers().get("X-Real_IP");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //http：//xxxx
        if (msg instanceof FullHttpRequest) {
            log.debug("收到消息，session={},msg={}", ctx.channel().id(), msg);
            FullHttpRequest fullHttpRequest = (FullHttpRequest) msg;
            String ip = getRealIp(fullHttpRequest);
            if (ip != null && ip.length() > 0) {
                if (ip.contains(":")) {
                    ip = ip.split(":")[0];
                } else if (ip.contains(",")) {
                    ip = ip.split(",")[0];
                }
                log.debug("ws 获取到用户真实ip={}", ip);
                AttributeKey<String> attributeKey = AttributeKey.valueOf("X-Real_IP");
                Attribute<String> attribute = ctx.channel().attr(attributeKey);
                attribute.set(ip);
            }
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            //ws://xxxx
            handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }


    public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        //关闭请求
        if (frame instanceof CloseWebSocketFrame) {
            channelInactive(ctx);
            return;
        }
        //ping请求
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        //支持二进制消息
        if (frame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            ByteBuf msg = frame.content();
            decode(ctx, msg);
        }
    }

    public void decode(ChannelHandlerContext ctx, ByteBuf msg) {
        int messageType = msg.readUnsignedShort();
        int cmd = msg.readUnsignedShort();
        byte[] array = new byte[msg.readableBytes()];
        msg.getBytes(msg.readerIndex(), array, 0, array.length);
        PFMessage message = new PFMessage(messageType, cmd, array);

        String sessionId = ctx.channel().id().asShortText();
        GateSession gateSession = GateSession.gateSessionMap.get(sessionId);
        if (gateSession != null) {
            gateSession.messageReceived(message);
        }
    }

    //第一次请求是http请求，请求头包括ws的信息
    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory("ws:/" + ctx.channel() + "/websocket", null, false);
        WebSocketServerHandshaker handshake = wsFactory.newHandshaker(req);
        if (handshake == null) {
            //不支持
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshake.handshake(ctx.channel(), req);
        }
    }


    public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
        // 返回应答给客户端
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }

        // 如果是非Keep-Alive，关闭连接
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }

    }

    private static boolean isKeepAlive(FullHttpRequest req) {
        return false;
    }


    //异常处理，netty默认是关闭channel
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
