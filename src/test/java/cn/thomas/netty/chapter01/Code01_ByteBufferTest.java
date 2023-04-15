package cn.thomas.netty.chapter01;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

import static cn.thomas.util.ByteBufferUtil.debugAll;

/**
 * @program: netty
 * @ClassName ByteBufferTest
 * @description: 测试ByteBuffer
 * @author: Thomas Ren
 * @create: 2023-04-09 09:40
 **/
@Slf4j
public class Code01_ByteBufferTest {

    @Test
    public void test_byteBuffer() {
        try (FileChannel channel = new FileInputStream("src/test/resources/data.txt").getChannel()) {
            // 准备缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(10);
            // 从channel读取数据，向buffer写入
            while (true) {
                int read = channel.read(buffer);
                log.debug("读取到字节数：{}", read);
                if (-1 == read) {
                    break;
                }
                // 打印buffer的内容
                // buffer切换为读模式
                buffer.flip();
                // 检查是否还有未读取数据
                while (buffer.hasRemaining()) {
                    char c = (char) buffer.get();
                    log.debug("读取字节：{}", c);
                }
                // buffer切换为写模式
                buffer.clear();
            }
        } catch (IOException e) {
            log.error("读取文件异常：", e);
        }
    }

    /**
     * 测试ByteBuffer读写功能
     */
    @Test
    public void test_byteBufferReadWrite() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put((byte) 0x61);
        debugAll(buffer);

        buffer.put(new byte[]{0x62, 0x63, 0x64});
        debugAll(buffer);

        // 切换成读模式
        buffer.flip();

        log.debug("读取字节：{}", (char) buffer.get());
        debugAll(buffer);

        // 切换写模式
        buffer.compact();
        debugAll(buffer);

        buffer.put((byte) 0x65);
        debugAll(buffer);
    }

    /**
     * 测试ByteBuffer内存分配
     */
    @Test
    public void test_allocate() {
        // class java.nio.HeapByteBuffer 堆内存，读写效率较低，受到垃圾回收影响
        log.debug("allocate分配ByteBuffer类型：{}", ByteBuffer.allocate(16).getClass());
        // class java.nio.DirectByteBuffer 直接内存，读写效率高（少一次拷贝），分配内存的效率较低，使用不当可能会造成内存泄漏
        log.debug("allocateDirect分配ByteBuffer类型：{}", ByteBuffer.allocateDirect(16).getClass());
    }

    /**
     * ByteBuffer读功能扩展
     */
    @Test
    public void test_byteBufferReadExtend() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put(new byte[]{'a', 'b', 'c', 'd'});
        buffer.flip();

        buffer.get(new byte[4]);
        debugAll(buffer);

        // 从头开始读，将position设置为0
        buffer.rewind();
        log.debug("rewind后读取buffer：{}", (char) buffer.get());
        debugAll(buffer);

        // mark & reset
        // mark：做一个标记，记录position位置，reset：将position设置为mark位置
        log.debug("rewind后读取buffer：{}", (char) buffer.get());
        log.debug("rewind后读取buffer：{}", (char) buffer.get());
        log.debug("mark当前position...");
        buffer.mark();
        log.debug("rewind后读取buffer：{}", (char) buffer.get());
        log.debug("回到mark位置");
        buffer.reset();
        debugAll(buffer);
        log.debug("rewind后读取buffer：{}", (char) buffer.get());

        // get(i)不会改变position的值
        log.debug("获取索引为1位置的值：{}", (char) buffer.get(1));
        debugAll(buffer);
    }

    /**
     * 测试ByteBuffer与字符串的转换
     */
    @Test
    public void test_ByteBufferString() {
        // 1. 字符串->ByteBuffer
        ByteBuffer buffer1 = ByteBuffer.allocate(16);
        buffer1.put("hello".getBytes());
        debugAll(buffer1);

        // 2. Charset 获取到的为读模式的buffer
        ByteBuffer buffer2 = StandardCharsets.UTF_8.encode("hello");
        debugAll(buffer2);

        // 3. wrap 获取到的为读模式的buffer
        ByteBuffer buffer3 = ByteBuffer.wrap("hello".getBytes());
        debugAll(buffer3);

        log.debug("Charset解析ByteBuffer：{}", StandardCharsets.UTF_8.decode(buffer2).toString());
        log.debug("未切换读模式：Charset解析ByteBuffer：{}", StandardCharsets.UTF_8.decode(buffer1).toString());
        buffer1.flip();
        log.debug("切换读模式：Charset解析ByteBuffer：{}", StandardCharsets.UTF_8.decode(buffer1).toString());
    }

    /**
     * 测试分散读取
     */
    @Test
    public void test_scatteringRead() {
        try (FileChannel channel = new RandomAccessFile("src/test/resources/words.txt", "r").getChannel()) {
            ByteBuffer buffer1 = ByteBuffer.allocate(3);
            ByteBuffer buffer2 = ByteBuffer.allocate(3);
            ByteBuffer buffer3 = ByteBuffer.allocate(5);
            channel.read(new ByteBuffer[]{buffer1, buffer2, buffer3});

            buffer1.flip();
            buffer2.flip();
            buffer3.flip();

            debugAll(buffer1);
            debugAll(buffer2);
            debugAll(buffer3);
        } catch (IOException e) {
        }
    }

    /**
     * 测试集中写入
     */
    @Test
    public void test_gatheringWrite() {
        try (FileChannel channel = new RandomAccessFile("src/test/resources/words-write.txt", "rw").getChannel()) {
            ByteBuffer buffer1 = StandardCharsets.UTF_8.encode("hello");
            ByteBuffer buffer2 = StandardCharsets.UTF_8.encode("world");
            ByteBuffer buffer3 = StandardCharsets.UTF_8.encode("你好");

            channel.write(new ByteBuffer[]{buffer1, buffer2, buffer3});

            debugAll(buffer1);
            debugAll(buffer2);
            debugAll(buffer3);
        } catch (IOException e) {
        }
    }

    /**
     * 粘包和半包
     * 网络上有多条数据发送给服务端，数据之间使用 \n 进行分隔
     * 但是由于某种原因这些数据再接受时，进行了重新组合，例如原始数据有三条：
     *  Hello,world\n
     *  I'm Thomas\n
     *  How are you?\n
     * 变成了下面两个 byteBuffer
     *  Hello,world\nI'm Thomas\nHo
     *  w are you?\n
     * 现在要求编写程序，将错乱的数据恢复成原始的按 \n 分隔的数据
     */
    @Test
    public void test_byteBufferExam() {
        ByteBuffer source = ByteBuffer.allocate(32);
        source.put("Hello,world\nI'm Thomas\nHo".getBytes());
        split(source);
        source.put("w are you?\n".getBytes());
        split(source);
    }

    private static void split(ByteBuffer source) {
        // 切换至读模式
        source.flip();
        for (int i = 0; i < source.limit(); i++) {
            // 找到完整消息
            if (source.get(i) == '\n') {
                int length = i + 1 - source.position();
                // 存储到新的ByteBuffer
                ByteBuffer target = ByteBuffer.allocate(length);
                // 从source读，向target写
                for (int j = 0; j < length; j++) {
                    target.put(source.get());
                }
                debugAll(target);
            }
        }

        // 切换至写模式，保留未读取剩余部分
        source.compact();
    }
}