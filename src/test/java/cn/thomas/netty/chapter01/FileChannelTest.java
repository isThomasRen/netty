package cn.thomas.netty.chapter01;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 任康芃
 * @date 2023/4/10 9:58
 * @description: FileChannel测试类
 */
@Slf4j
public class FileChannelTest {

    /**
     * 测试transform，底层使用零拷贝，效率较高
     * transformTo：源端调用，向目标端传输数据
     * transformFrom：目标端调用，从源端拉取数据
     */
    @Test
    public void test_transform() {
        String from = "src/test/resources/data.txt";
        String to = "src/test/resources/data_transform.txt";

        try (FileChannel fromChannel = new FileInputStream(from).getChannel();
             FileChannel toChannel = new FileOutputStream(to).getChannel()) {
            long size = fromChannel.size();
            for (long left = size; left > 0; ) {
                log.debug("position: {}, left: {}", size - left, left);
                // 每次最多只能传输2G内容
                left -= fromChannel.transferTo(size - left, (left), toChannel);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * JDK 1.7引入了Path和Paths类
     * Path用来表示文件路径
     * Paths是工具类，用来获取Path实例
     */
    @Test
    public void test_path() {
        Path path = Paths.get("src/test/resources/data.txt");
        System.out.println(path);

        Path resources = Paths.get("src/test", "resources");
        System.out.println(resources);
    }

    /**
     * JDK 1.7引入了Files类，提供便捷的文件操作
     *
     * @throws IOException
     */
    @Test
    public void test_file() throws IOException {
        // 检查文件是否存在
        Path path = Paths.get("src/test/resources/data.txt");
        log.debug("文件是否存在：{}", Files.exists(path));

        // 创建一级目录
        log.debug("创建一级目录：{}", path);
        path = Paths.get("src/test/resources/dir");
        Files.createDirectory(path);

        // 创建多级目录
        log.debug("创建多级目录：{}", path);
        path = Paths.get("src/test/resources/dir/d1/d2");
        Files.createDirectories(path);

        // 拷贝文件
        Path source = Paths.get("src/test/resources/data.txt");
        Path target = Paths.get("src/test/resources/data_copy.txt");
        log.debug("拷贝文件，source：{}, target：{}", source, target);
        Files.copy(source, target);

        // 移动文件
        source = Paths.get("src/test/resources/data_copy.txt");
        target = Paths.get("src/test/resources/data_move.txt");
        log.debug("移动文件，source：{}, target：{}", source, target);
        Files.move(source, target);

        // 删除文件
        Path deletePath = Paths.get("src/test/resources/data_move.txt");
        log.debug("删除文件：{}", deletePath);
        Files.delete(deletePath);

        // 删除目录
        Path deleteDir = Paths.get("src/test/resources/dir/d1/d2");
        log.debug("删除目录：{}", deleteDir);
        Files.delete(deleteDir);
        deleteDir = Paths.get("src/test/resources/dir/d1");
        log.debug("删除目录：{}", deleteDir);
        Files.delete(deleteDir);
        deleteDir = Paths.get("src/test/resources/dir");
        log.debug("删除目录：{}", deleteDir);
        Files.delete(deleteDir);
    }

    /**
     * 测试Files类中的walk相关方法，遍历文件目录
     */
    @Test
    public void test_fileWalk() throws IOException {
        String javaHome = System.getenv("JAVA_HOME");
        Path path = Paths.get(javaHome);
        AtomicInteger dirCount = new AtomicInteger();
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger jarCount = new AtomicInteger();
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                log.debug("访问目录：{}", dir);
                dirCount.getAndIncrement();
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.debug("访问文件：{}", file);
                if (file.getFileName().toString().endsWith(".jar")) {
                    jarCount.getAndIncrement();
                }
                fileCount.getAndIncrement();
                return super.visitFile(file, attrs);
            }
        });
        log.info("目录总数：{}", dirCount);
        log.info("文件总数：{}", fileCount);
        log.info("jar总数：{}", jarCount);
    }

    /**
     * 通过 Files.walkFileTree 方法拷贝目录
     *
     * @throws IOException
     */
    @Test
    public void test_fileWalkAndCopy() throws IOException {
        String javaHome = System.getenv("JAVA_HOME");
        Path path = Paths.get(javaHome);
        String targetPre = "src/test/resources/java_home";
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String sourceName = dir.toString();
                String targetName = sourceName.replace(javaHome, targetPre);
                log.debug("拷贝目录：{} -> {}", sourceName, targetName);
                Files.createDirectory(Paths.get(targetName));
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String sourceName = file.toString();
                String targetName = sourceName.replace(javaHome, targetPre);
                log.debug("拷贝文件：{} -> {}", sourceName, targetName);
                Files.copy(file, Paths.get(targetName));
                return super.visitFile(file, attrs);
            }
        });
    }

    /**
     * 通过 Files.walkFileTree 方法删除目录
     *
     * @throws IOException
     */
    @Test
    public void test_fileWalkAndDelete() throws IOException {
        Path path = Paths.get("src/test/resources/java_home");
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                log.debug("删除目录：{}", dir);
                Files.delete(dir);
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // classes.jsa文件无法删除
                log.debug("删除文件：{}", file);
                Files.delete(file);
                return super.visitFile(file, attrs);
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.debug("访问文件：{} 失败", file);
                return super.visitFileFailed(file, exc);
            }
        });
    }
}
