/*
 * Copyright (c) 2017. Chengdu Qianxing Technology Co.,LTD.
 * All Rights Reserved.
 */

package org.alan.mars.cluster;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Created on 2017/3/31.
 *
 * @author Alan
 * @since 1.0
 */
public class ClusterConnectWorkPoolInitializer extends ChannelInitializer<SocketChannel> {
    private static final int MSG_MAX_SIZE = 10 * 1024 * 1024;

    private static final int HEADER_SIZE = 4;
    private final ClusterMessageDispatcher clusterMessageDispatcher;

    public ClusterConnectWorkPoolInitializer(ClusterMessageDispatcher clusterMessageDispatcher) {
        this.clusterMessageDispatcher = clusterMessageDispatcher;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                MSG_MAX_SIZE, 0, HEADER_SIZE, 0, 4))
                .addLast(new ClusterMessageDecoder())
                .addLast(new LengthFieldPrepender(HEADER_SIZE))
                .addLast(new ClusterMessageEncoder())
                .addLast("idleStateHandler", new IdleStateHandler(60, 20, 30, TimeUnit.SECONDS))
                .addLast(new NioEventLoopGroup(), new ClusterConnect(clusterMessageDispatcher));
    }
}
