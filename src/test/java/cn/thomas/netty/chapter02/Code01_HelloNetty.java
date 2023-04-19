package cn.thomas.netty.chapter02;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;

/**
 * @author 任康芃
 * @date 2023/4/19 14:26
 * @description: Netty测试类
 */
@Slf4j
public class Code01_HelloNetty {

    @Test
    public void test_nioServer() throws IOException {
        new ServerBootstrap()
                // 创建NioEventLoopGroup，可以简单理解成线程池与Selector
                .group(new NioEventLoopGroup())
                // 选择Socket实现类，其中NioServerSocketChannel表示基于NIO的服务器端实现
                .channel(NioServerSocketChannel.class)
                // 给SocketChannel添加处理器，ChannelInitializer处理器仅执行一次，客户端SocketChannel建立连接后，执行initChannel以便添加更多的处理器
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline().addLast(new StringDecoder());
                        nioSocketChannel.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
                                log.debug("接收到客户端 [{}] 发送消息：{}", nioSocketChannel, msg);
                            }
                        });
                    }
                })
                // 绑定监听端口
                .bind(8080);
        System.in.read();
    }

    @Test
    public void test_nioClient() throws InterruptedException, IOException {
        new Bootstrap()
                // 同服务器端，创建NioEventLoopGroup
                .group(new NioEventLoopGroup())
                // 选择Socket实现类
                .channel(NioSocketChannel.class)
                // 添加SocketChannel的处理器，ChannelInitializer处理器仅执行一次，客户端SocketChannel建立连接后，执行initChannel以便添加更多的处理器
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new StringEncoder());
                    }
                })
                // 指定连接的服务器地址即端口
                .connect("localhost", 8080)
                // netty中很多方法都是异步的，如connect，这时需要使用sync方法等待connect建立连接完毕
                .sync()
                // 获取channel对象，它即为通道抽象，可以及进行数据读写操作
                .channel()
                // 写入消息并清空缓冲区
                .writeAndFlush("hello netty!");
        System.in.read();
    }
}
