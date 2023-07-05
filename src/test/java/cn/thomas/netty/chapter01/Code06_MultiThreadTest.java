package cn.thomas.netty.chapter01;

import cn.thomas.util.ByteBufferUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: netty
 * @ClassName Code06_MultiThreadTest
 * @description: 多线程测试
 * @author: Thomas Ren
 * @create: 2023-04-15 16:35
 **/
@Slf4j
public class Code06_MultiThreadTest {

    @Test
    public void test_client() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 8080));
        socketChannel.write(ByteBuffer.wrap("hello world\n".getBytes(StandardCharsets.UTF_8)));
        log.debug("debug...");
    }

    @Test
    public void test_multiThreadServer() throws IOException {
        new BossEventLoop().register();
    }

    @Slf4j
    static class BossEventLoop implements Runnable {

        private Selector boss;
        private WorkerEventLoop[] workerEventLoops;
        private volatile boolean start = false;
        private final AtomicInteger index = new AtomicInteger();

        public void register() throws IOException {
            if (!start) {
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.bind(new InetSocketAddress(8080));
                serverSocketChannel.configureBlocking(false);

                boss = Selector.open();
                serverSocketChannel.register(boss, SelectionKey.OP_ACCEPT);

                workerEventLoops = initWorkerEventLoops();

                log.debug("boss线程启动...");
                new Thread(this, "boss").start();
                start = true;
            }
        }

        private WorkerEventLoop[] initWorkerEventLoops() {
            workerEventLoops = new WorkerEventLoop[Runtime.getRuntime().availableProcessors()];
            for (int i = 0; i < workerEventLoops.length; i++) {
                WorkerEventLoop workerEventLoop = new WorkerEventLoop("worker-" + i);
                workerEventLoops[i] = workerEventLoop;
            }
            return workerEventLoops;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    boss.select();
                    Set<SelectionKey> selectionKeys = boss.selectedKeys();
                    Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey selectionKey = selectionKeyIterator.next();
                        selectionKeyIterator.remove();
                        if (selectionKey.isAcceptable()) {
                            ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
                            SocketChannel socketChannel = channel.accept();
                            socketChannel.configureBlocking(false);
                            log.debug("接收到客户端连接：{}", socketChannel);
                            // 负载均衡 - 轮询
                            workerEventLoops[index.getAndIncrement() % workerEventLoops.length].register(socketChannel);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Slf4j
    static class WorkerEventLoop implements Runnable {

        private final String name;
        private Selector worker;
        private volatile boolean start = false;

        private ConcurrentLinkedDeque<Runnable> tasks = new ConcurrentLinkedDeque<>();

        public WorkerEventLoop(String name) {
            this.name = name;
        }

        public void register(SocketChannel socketChannel) throws IOException {
            if (!start) {
                worker = Selector.open();
                new Thread(this, name).start();
                start = true;
            }
            // 向队列条件任务，并没有立刻执行
            tasks.add(() -> {
                try {
                    socketChannel.register(worker, SelectionKey.OP_READ, ByteBuffer.allocate(4));
                } catch (ClosedChannelException e) {
                    e.printStackTrace();
                }
            });
            // 唤醒selector，此时已经手动向队列中入队了一个任务，唤醒被select()阻塞的worker
            worker.wakeup();
        }

        @Override
        public void run() {
            log.debug("worker线程启动...");
            while (true) {
                try {
                    // 阻塞的，通常情况下，只有注册再selector上的channel有事件就绪时，select()才会从阻塞中被唤醒
                    worker.select();
                    Runnable task = tasks.poll();
                    if (null != task) {
                        // 执行注册
                        task.run();
                    }
                    Set<SelectionKey> selectionKeys = worker.selectedKeys();
                    Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey selectionKey = selectionKeyIterator.next();
                        selectionKeyIterator.remove();
                        // 处理可读事件
                        if (selectionKey.isReadable()) {
                            SocketChannel channel = (SocketChannel) selectionKey.channel();
                            log.debug("接收到客户端 {} 发送的消息", channel);
                            ByteBuffer byteBuffer = (ByteBuffer) selectionKey.attachment();
                            try {
                                int read = channel.read(byteBuffer);
                                if (-1 == read) {
                                    selectionKey.cancel();
                                    channel.close();
                                }
                                resolveMessage(byteBuffer);
                                if (byteBuffer.position() == byteBuffer.limit()) {
                                    ByteBuffer newByteBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2);
                                    byteBuffer.flip();
                                    newByteBuffer.put(byteBuffer);
                                    selectionKey.attach(newByteBuffer);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                selectionKey.cancel();
                                channel.close();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void resolveMessage(ByteBuffer source) {
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            if ('\n' == source.get(i)) {
                int length = i - source.position() + 1;
                ByteBuffer target = ByteBuffer.allocate(length);
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                target.flip();
                ByteBufferUtil.debugAll(target);
                break;
            }
        }
        source.compact();
    }
}