package cn.thomas.netty.chapter01;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * @program: netty
 * @ClassName Code05_WriteTest
 * @description: 测试写事件
 * @author: Thomas Ren
 * @create: 2023-04-15 11:48
 **/
@Slf4j
public class Code05_WriteTest {

    /**
     * 测试读事件
     *
     * @throws IOException
     */
    @Test
    public void test_writeServer() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress("localhost", 8080));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            while (selectionKeyIterator.hasNext()) {
                SelectionKey selectionKey = selectionKeyIterator.next();
                selectionKeyIterator.remove();

                if (selectionKey.isAcceptable()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    SelectionKey socketChannelSelectKey = socketChannel.register(selector, 0);
                    socketChannelSelectKey.interestOps(SelectionKey.OP_READ);

                    // 向客户端发送消息
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < 3000000; i++) {
                        stringBuilder.append("a");
                    }
                    ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(stringBuilder.toString());
                    // 向客户端发送消息，消息长度 【三百万】 字节
                    int write = socketChannel.write(byteBuffer);
                    log.info("实际写入字节数：{}", write);
                    // 如果还有剩余字节数
                    if (byteBuffer.hasRemaining()) {
                        // 将客户端channel关注事件添加读事件，并将剩余的字节数组添加到附件中
                        socketChannelSelectKey.interestOps(socketChannelSelectKey.interestOps() | SelectionKey.OP_WRITE);
                        socketChannelSelectKey.attach(byteBuffer);
                    }
                }
                // 处理写事件
                if (selectionKey.isWritable()) {
                    ByteBuffer byteBuffer = (ByteBuffer) selectionKey.attachment();
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    int write = socketChannel.write(byteBuffer);
                    log.info("实际写入字节数：{}", write);
                    // 如果不再剩余字节
                    if (!byteBuffer.hasRemaining()) {
                        // 将客户端channel关注的事件去掉读事件，并清空附件
                        selectionKey.interestOps(selectionKey.interestOps() ^ SelectionKey.OP_WRITE);
                        selectionKey.attach(null);
                    }
                }
            }
        }
    }

    /**
     * 测试读事件客户端
     *
     * @throws IOException
     */
    @Test
    public void test_client() throws IOException {
        Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
        socketChannel.connect(new InetSocketAddress("localhost", 8080));

        int count = 0;
        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            while (selectionKeyIterator.hasNext()) {
                SelectionKey selectionKey = selectionKeyIterator.next();
                selectionKeyIterator.remove();
                if (selectionKey.isConnectable()) {
                    log.debug("客户端完成连接：{}", socketChannel.finishConnect());
                }
                if (selectionKey.isReadable()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    count += socketChannel.read(byteBuffer);
                    byteBuffer.clear();
                    log.debug("接受到服务器消息次数：{}", count++);
                }
            }
        }
    }

}