/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.alan.mars.net.Connect;
import org.alan.mars.net.ConnectListener;
import org.alan.mars.message.NetAddress;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * netty 连接抽象类
 * <p>
 * Created on 2017/3/31.
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public abstract class NettyConnect extends SimpleChannelInboundHandler<Object> implements Connect {

    protected ChannelHandlerContext ctx;
    protected NetAddress remoteAddress;
    protected List<ConnectListener> connectListeners = new CopyOnWriteArrayList<>();
    protected long lastHeartbeatTime;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.ctx = ctx;
        log.debug("连接创建完成,ctx={}", ctx);
        InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
        remoteAddress = new NetAddress(address.getAddress().getHostAddress(), address.getPort());
        onCreate();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        log.error("tips caught exception,ctx=" + ctx, cause);
        close();
    }

    @Override
    public boolean write(Object msg) {
        try {
            ctx.writeAndFlush(msg).addListener(future -> {
                if (!future.isSuccess()) {
                    Throwable e = future.cause();
                    if (e != null) {
                        e.printStackTrace();
                        log.error("", e);
                    }
                }
            });
        } catch (Exception e) {
            log.error("", e);
        }

        return true;
    }

    @Override
    public boolean isActive() {
        return ctx.channel().isActive();
    }

    @Override
    public NetAddress address() {
        return remoteAddress;
    }

    @Override
    public void close() {
        log.debug("服务器主动关闭连接,netAddress={},ctx={}", remoteAddress, ctx);
        try {
            ctx.close();
        } catch (Exception e) {
            log.warn("关闭连接异常,netAddress=" + remoteAddress + ",ctx=" + ctx, e);
        }

    }


    public void writeAndClose(Object obj) {
        log.debug("服务器主动关闭连接并通知,netAddress={},ctx={}", remoteAddress, ctx);
        try {
            ctx.writeAndFlush(obj).addListener(future -> {
               if (isActive()){
                   ctx.close();
               }
            });
        } catch (Exception e) {
            log.warn("关闭连接异常,netAddress=" + remoteAddress + ",ctx=" + ctx, e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        messageReceived(msg);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("管道中断,ctx={}", ctx);
        connectListeners.forEach(connectListener -> connectListener.onConnectClose(this));
        connectListeners.clear();
        onClose();
        super.channelInactive(ctx);
    }

    /**
     * 当消息到达
     */
    public abstract void messageReceived(Object msg);

    @Override
    public void addConnectListener(ConnectListener connectListener) {
        connectListeners.add(connectListener);
    }

    @Override
    public void removeConnectListener(ConnectListener connectListener) {
        connectListeners.remove(connectListener);
    }

    @Override
    public String toString() {
        return "NettyConnect{" +
                "ctx=" + ctx +
                ", remoteAddress=" + remoteAddress +
                '}';
    }
}
