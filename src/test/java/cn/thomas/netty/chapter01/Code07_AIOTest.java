package cn.thomas.netty.chapter01;

import cn.thomas.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * @author 任康芃
 * @date 2023/4/19 10:17
 * @description: AIO代码测试
 */
@Slf4j
public class Code07_AIOTest {

    @Test
    public void test_fileIO() throws IOException {
        try {
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(Paths.get("src/test/resources/data.txt"), StandardOpenOption.READ);
            ByteBuffer buffer = ByteBuffer.allocate(2);
            log.debug("begin...");
            fileChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    log.debug("read completed... {}", result);
                    buffer.flip();
                    ByteBufferUtil.debugAll(buffer);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    log.debug("read failed...");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug("do other things...");
        System.in.read();
    }

    @Test
    public void test_aioServer() throws IOException {
        AIOServer aioServer = new AIOServer();
        aioServer.startAIOServer();
    }

    @Slf4j
    static class AIOServer {

        public void startAIOServer() throws IOException {
            AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(8080));
            serverSocketChannel.accept(null, new AcceptHandler(serverSocketChannel));
            System.in.read();
        }

        private static void closeChannel(AsynchronousSocketChannel socketChannel) {
            try {
                log.debug("关闭通道：{}", socketChannel);
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private static class ReadHandler implements CompletionHandler<Integer, ByteBuffer> {

            private AsynchronousSocketChannel socketChannel;

            public ReadHandler(AsynchronousSocketChannel socketChannel) {
                this.socketChannel = socketChannel;
            }

            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                try {
                    if (-1 == result) {
                        closeChannel(socketChannel);
                        return;
                    }
                    attachment.flip();
                    log.debug("获取到客户端 [{}] 消息：{}", socketChannel, Charset.defaultCharset().decode(attachment));
                    attachment.clear();
                    // 处理完第一个 read 时，需要再次调用 read 方法来处理下一次 read 事件
                    socketChannel.read(attachment, attachment, this);
                } catch (Exception e) {
                    e.printStackTrace();
                    closeChannel(socketChannel);
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
            }
        }

        private static class WriteHandler implements CompletionHandler<Integer, ByteBuffer> {

            private AsynchronousSocketChannel socketChannel;

            public WriteHandler(AsynchronousSocketChannel socketChannel) {
                this.socketChannel = socketChannel;
            }

            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (attachment.hasRemaining()) {
                    socketChannel.write(attachment);
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
                closeChannel(socketChannel);
            }
        }

        private static class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {

            private AsynchronousServerSocketChannel serverSocketChannel;

            public AcceptHandler(AsynchronousServerSocketChannel serverSocketChannel) {
                this.serverSocketChannel = serverSocketChannel;
            }

            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Object attachment) {
                try {
                    log.debug("获取到连接信息：{}", socketChannel);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                ByteBuffer buffer = ByteBuffer.allocate(16);
                // 读事件由 ReadHandler 处理
                socketChannel.read(buffer, buffer, new ReadHandler(socketChannel));
                // 写事件由 WriteHandler 处理
                socketChannel.write(Charset.defaultCharset().encode("server hello!"), ByteBuffer.allocate(16), new WriteHandler(socketChannel));

                // 连接完成后，调用此方法处理下一次连接事件
                serverSocketChannel.accept(null, this);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                exc.printStackTrace();
            }
        }
    }
}
