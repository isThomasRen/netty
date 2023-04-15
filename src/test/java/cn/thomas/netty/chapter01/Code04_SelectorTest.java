package cn.thomas.netty.chapter01;

import cn.thomas.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @program: netty
 * @ClassName SelectorTest
 * @description: 选择器测试类
 * @author: Thomas Ren
 * @create: 2023-04-15 09:48
 **/
@Slf4j
public class Code04_SelectorTest {

    /**
     * 测试多路复用模式服务器
     *
     * @throws IOException
     */
    @Test
    public void test_serverWithSelector() throws IOException {
        // 开启监听器
        Selector selector = Selector.open();

        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(8080));
        serverSocketChannel.configureBlocking(false);

        // 将serverSocketChannel注册到选择器上，监听连接事件
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            // 对select进行轮询
            selector.select();
            // 获取所有未处理事件
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
            while (selectionKeyIterator.hasNext()) {
                SelectionKey selectionKey = selectionKeyIterator.next();
                selectionKeyIterator.remove();
                // 如果是连接事件
                if (selectionKey.isAcceptable()) {
                    ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
                    SocketChannel socketChannel = ssc.accept();
                    socketChannel.configureBlocking(false);
                    log.debug("获取到客户端连接：{}", socketChannel);
                    // 注册到选择器上，监听读事件，并绑定一个ByteBuffer作为附件
                    SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4));
                    key.interestOps(SelectionKey.OP_READ);
                }
                // 如果是读取事件
                if (selectionKey.isReadable()) {
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    // 获取到绑定的附件ByteBuffer
                    ByteBuffer byteBuffer = (ByteBuffer) selectionKey.attachment();
                    try {
                        // 客户端强制断开连接后，继续读取会发生异常，进行捕获，处理客户端强制退出逻辑
                        int read = socketChannel.read(byteBuffer);
                        // 客户端正常退出后，会发送长度为-1的度时间，在此处处理断开连接的逻辑
                        if (-1 == read) {
                            log.debug("客户端：{} 断开连接", socketChannel);
                            // 客户端断开连接，忽略本次读事件
                            selectionKey.cancel();
                        }
                        // 读取客户端发送的消息
                        else {
                            spilt(byteBuffer);
                            // ByteBuffer进行扩容
                            if (byteBuffer.position() == byteBuffer.limit()) {
                                ByteBuffer newByteBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2);
                                byteBuffer.flip();
                                newByteBuffer.put(byteBuffer);
                                selectionKey.attach(newByteBuffer);
                            }
                        }
                    } catch (IOException e) {
                        log.warn("读取客户端：{} 数据失败：", socketChannel, e);
                        selectionKey.cancel();
                    }
                }
            }
        }
    }

    private void spilt(ByteBuffer source) {
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            char c = (char) source.get(i);
            if ('\n' == c) {
                int length = i + 1 - source.position();
                ByteBuffer target = ByteBuffer.allocate(length);
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                target.flip();
                ByteBufferUtil.debugAll(target);
            }
        }
        source.compact();
    }

    /**
     * 测试客户端
     *
     * @throws IOException
     */
    @Test
    public void test_client() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8080));
        log.debug("debug...");
    }
}