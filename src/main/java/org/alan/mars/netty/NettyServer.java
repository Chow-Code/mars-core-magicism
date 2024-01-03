package org.alan.mars.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 采用Netty4.x 实现NIO服务器，该类继承了Thread 线程类，应用过程中如果需要
 * <p>
 * 异步开启网络服务，可以调用start方法，否则直接调用run。
 * <p>
 * <p>
 *
 * @author Alan
 * @since 1.0
 */
@Slf4j
public class NettyServer extends Thread {
    /**
     * 服务监听端口
     */
    private final int portNumber;
    /**
     * 服务监听地址
     */
    private final String address;
    /**
     * 连接初始化器
     */
    private final ChannelInitializer<SocketChannel> initializer;

    public NettyServer(int portNumber, ChannelInitializer<SocketChannel> initializer) {
        this(null, portNumber, initializer);
    }

    public NettyServer(String address, int portNumber, ChannelInitializer<SocketChannel> initializer) {
        super("netty-server-" + portNumber);
        this.address = address;
        this.portNumber = portNumber;
        this.initializer = initializer;
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors()+1);
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        //workerGroup.setIoRatio(30);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(initializer).childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_BACKLOG, 4096)//建议设置到4K
                    .handler(new LoggingHandler(LogLevel.INFO));
            // 服务器绑定端口监听
            ChannelFuture f;
            if (address != null) {
                f = b.bind(address, portNumber).sync();
                log.info("Server started on address " + address + ", port "
                        + portNumber);
            } else {
                f = b.bind(portNumber).sync();
                log.info("Server started on port " + portNumber);
            }
            // 监听服务器关闭监听
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("\nnet server start error...\n", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
