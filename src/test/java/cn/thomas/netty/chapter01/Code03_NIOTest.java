package cn.thomas.netty.chapter01;

import cn.thomas.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 任康芃
 * @date 2023/4/12 9:16
 * @description: NIO单元测试
 */
@Slf4j
public class Code03_NIOTest {

    /**
     * 阻塞模式NIO服务器端
     *
     * @throws IOException
     */
    @Test
        public void test_blockingNIOServer() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));

        List<SocketChannel> socketChannels = new ArrayList<>();
        while (true) {
            log.debug("serverSocketChannel等待连接...");
            SocketChannel socketChannel = serverSocketChannel.accept();
            log.debug("serverSocketChannel连接成功：{}", socketChannel);
            socketChannels.add(socketChannel);
            for (SocketChannel channel : socketChannels) {
                log.debug("准备读取socketChannel: {} 中内容", channel);
                channel.read(byteBuffer);
                log.debug("读取socketChannel: {} 中内容完成", channel);
                byteBuffer.flip();
                ByteBufferUtil.debugRead(byteBuffer);
                byteBuffer.clear();
            }
        }
    }

    /**
     * 非阻塞模式NIO服务器
     *
     * @throws IOException
     */
    @Test
    public void test_nonblockingNioServer() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false);

        List<SocketChannel> socketChannels = new ArrayList<>();
        while (true) {
            SocketChannel socketChannel = serverSocketChannel.accept();
            if (null != socketChannel) {
                socketChannel.configureBlocking(false);
                log.debug("客户端连接完成：{}", socketChannel);
                socketChannels.add(socketChannel);
            }
            for (SocketChannel channel : socketChannels) {
                int read = channel.read(byteBuffer);
                if (read > 0) {
                    log.debug("读取客户端 {} 完成", channel);
                    byteBuffer.flip();
                    ByteBufferUtil.debugRead(byteBuffer);
                    byteBuffer.clear();
                }
            }
        }
    }

    /**
     * NIO客户端
     *
     * @throws IOException
     */
    @Test
    public void test_nioClient() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8080));
        System.out.println("debug...");
    }

}
