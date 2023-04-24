package cn.thomas.netty.chapter02;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author 任康芃
 * @date 2023/4/24 10:28
 * @description: 测试Channel
 */
@Slf4j
public class Code03_ChannelTest {

    @Test
    public void test_nettyServer() throws IOException {
        new ServerBootstrap()
                .group(new NioEventLoopGroup(), new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf byteBuf = msg instanceof ByteBuf ? (ByteBuf) msg : null;
                                if (null != byteBuf) {
                                    log.debug("接收到客户端[ {} ]发送的消息：{}", ch, byteBuf.toString(StandardCharsets.UTF_8));
                                }
                            }
                        });
                    }
                })
                .bind(8080);
        System.in.read();
    }

    @Test
    public void test_nettyClient() throws IOException {
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture channelFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        log.debug("client channel init...");
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    }
                })
                .connect("localhost", 8080);
        /* channelFuture.sync(); */
        Channel channel = channelFuture.channel();
        log.debug("channel对象：{}", channel);
        channelFuture.addListener((ChannelFutureListener) future -> log.debug("channel对象：{}", future.channel()));
        log.debug("channel对象：{}", channel);

        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                channel.writeAndFlush(ByteBufAllocator.DEFAULT.buffer().writeBytes(("hello " + i).getBytes()));
            }
            channel.close();
        }, "input").start();

        // 获取CloseFuture对象，1. 同步处理关闭，2. 异步处理关闭
        ChannelFuture closeFuture = channel.closeFuture();
        /*closeFuture.sync();
        log.debug("处理关闭之后的操作");*/
        closeFuture.addListener((ChannelFutureListener) future -> {
            log.debug("处理关闭之后的操作...");
            group.shutdownGracefully();
        });
        System.in.read();
    }
}
