package cn.thomas.netty.chapter02;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author 任康芃
 * @date 2023/4/20 22:00
 * @description: EventLoop测试
 */
@Slf4j
public class Code02_EventLoopTest {

    @Test
    public void test_eventLoop() {
        DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(2);

        log.debug("next:{}", defaultEventLoopGroup.next());
        log.debug("next:{}", defaultEventLoopGroup.next());
        log.debug("next:{}", defaultEventLoopGroup.next());

        // 处理普通事件
        EventLoop eventLoop = defaultEventLoopGroup.next();
        eventLoop.submit(() -> log.debug("thread running..."));

        // 处理定时事件
        eventLoop.scheduleAtFixedRate(() -> log.debug("thread running"), 1, 1, TimeUnit.SECONDS);
        log.debug("main running");
    }

    @Test
    public void test_nioEventLoopServer() throws IOException {
        DefaultEventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup(2);
        new ServerBootstrap()
                // 绑定两个EventLoopGroup，第一个用于处理连接事件，第二个用于处理读写事件
                .group(new NioEventLoopGroup(), new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline()
                                // 绑定IO读写事件
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        ByteBuf byteBuf = msg instanceof ByteBuf ? (ByteBuf) msg : null;
                                        if (null != byteBuf) {
                                            log.debug("接收到客户端[ {} ]发送的消息：{}", ch, byteBuf.toString(StandardCharsets.UTF_8));
                                        }
                                        // 如果后续仍有处理器，需要调用此方法唤醒后续处理器
                                        ctx.fireChannelRead(msg);
                                    }
                                })
                                // 耗时的操作可以绑定到默认EventLoopGroup，减轻NioEventLoopGroup的工作负担，提升IO吞吐量
                                .addLast(defaultEventLoopGroup, "defaultHandler", new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        ByteBuf byteBuf = msg instanceof ByteBuf ? (ByteBuf) msg : null;
                                        if (null != byteBuf) {
                                            log.debug("DefaultEventLoopGroup 接收到客户端[ {} ]发送的消息：{}", ch, byteBuf.toString(StandardCharsets.UTF_8));
                                        }
                                    }
                                });
                    }
                })
                .bind(8080);
        System.in.read();
    }

    @Test
    public void test_nioEventLoopClient() throws InterruptedException, IOException {
        Channel channel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        log.debug("client channel init...");
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                    }
                })
                .connect("localhost", 8080)
                .sync()
                .channel();
        channel.writeAndFlush(ByteBufAllocator.DEFAULT.buffer().writeBytes("hello world".getBytes()));
        System.in.read();
    }
}
