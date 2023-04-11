# NIO（Non-blocking IO）
## ByteBuffer

> `Buffer`是非线程安全的

相关代码见：<u>cn.thomas.netty.chapter01.ByteBufferTest</u>

### 常用方法

* **读写功能：<u>ByteBufferTest#test_byteBufferReadWrite()</u>**

  写入：

  ```java
  // 写入单个字节
  buffer.put((byte) 0x61);
  // 写入字节数组
  buffer.put(new byte[]{0x62, 0x63, 0x64});
  ```

  读出：

  ```java
  // 获取buffer中一个字节
  buffer.get()
  ```

* **模式切换：<u>ByteBufferTest#test_byteBufferReadWrite()</u>**

  `ByteBuffer`实例化后默认为写模式

  ```java
  // 切换成读模式
  buffer.flip();
  
  // 切换成写模式
  buffer.clear();		// 清空buffer数组，position设置为0
  buffer.compact();	// 保留未读取的字节
  ```

* **内存分配：<u>ByteBufferTest#test_allocate()</u>**

  可分配堆内存，或分配直接内存

  ```java
  // class java.nio.HeapByteBuffer 堆内存，读写效率较低，受到垃圾回收影响
  ByteBuffer buffer = ByteBuffer.allocate(16);
  // class java.nio.DirectByteBuffer 直接内存，读写效率高（少一次拷贝），分配内存的效率较低，使用不当可能会造成内存泄漏
  ByteBuffer buffer = ByteBuffer.allocateDirect(16);
  ```

* **读功能扩展：<u>ByteBufferTest#test_byteBufferReadExtend</u>**

  `mark()`&`reset()`

  ```java
  // 标记当前position位置
  buffer.mark();
  // 将position值设置会mark标记位置
  buffer.reset();
  ```

  `get()`&`get(i)`

  ```java
  // 获取position位置的字节，并且position自增
  buffer.get();
  // 获取指定下标索引位置的字节，不改变position位置的值
  buffer.get(i);
  ```

* **与字符串的转换：<u>ByteBufferTest#test_ByteBufferString()</u>**

  字符串 转化为 `ByteBuffer`：

  ```java
  ByteBuffer buffer = ByteBuffer.allocate(16);
  buffer.put("hello".getBytes());
  ```

  通过`Charset`获取读模式的`ByteBuffer`：

  ```java
  ByteBuffer buffer = StandardCharsets.UTF_8.encode("hello");
  ```

  通过`ByteBuffer.warp()`方法获取读模式的`ByteBuffer`：

  ```java
  ByteBuffer buffer = ByteBuffer.wrap("hello".getBytes());
  ```

  `ByteBuffer`转化为字符串：

  ```java
  StandardCharsets.UTF_8.decode(buffer).toString()
  ```

* **分散读取&集中写入：**

  分散读取：**<u>ByteBufferTest#test_scatteringRead()</u>**

  ```java
  ByteBuffer buffer1 = ByteBuffer.allocate(3);
  ByteBuffer buffer2 = ByteBuffer.allocate(3);
  ByteBuffer buffer3 = ByteBuffer.allocate(5);
  
  channel.read(new ByteBuffer[]{buffer1, buffer2, buffer3});
  ```

  集中写入：**<u>ByteBufferTest#test_gatheringWrite()</u>**

  ```java
  ByteBuffer buffer1 = StandardCharsets.UTF_8.encode("hello");
  ByteBuffer buffer2 = StandardCharsets.UTF_8.encode("world");
  ByteBuffer buffer3 = StandardCharsets.UTF_8.encode("你好");
  
  channel.write(new ByteBuffer[]{buffer1, buffer2, buffer3});
  ```

### 结构

`ByteBuffer`有以下三个重要属性：

* capacity：容量
* limit：读写限制
* position：读写指针

1. **初始化后：**

   ![image-20230411094621985](README.assets/image-20230411094621985.png)

2. **写模式下，position 是写入位置，limit 等于容量，下图表示写入了 4 个字节后的状态**

   ![image-20230411094647339](README.assets/image-20230411094647339.png)

3. **flip 动作发生后，position 切换为读取位置，limit 切换为读取限制**

   ![image-20230411094704355](README.assets/image-20230411094704355.png)

4. **读取 4 个字节后，状态**

   ![image-20230411094731962](README.assets/image-20230411094731962.png)

5. **clear 动作发生后，状态**

   ![image-20230411094745827](README.assets/image-20230411094745827.png)

6. **compact 方法，是把未读完的部分向前压缩，然后切换至写模式**

   ![image-20230411094807941](README.assets/image-20230411094807941.png)



## 文件编程

相关代码见：<u>cn.thomas.netty.chapter01.FileChannelTest</u>

### FileChannel

> `FileChannel`只能在阻塞模式下工作

#### 常用方法

1. **获取：**

   不能直接打开`FileChannel`，必须通过`FileInputStream`、`FileOutputStream`或者`RandomAccessFile`来获取`FileChannel`，它们都提供了`getChannel()`方法

   * 通过`FileInputStream.getChannel()`获取的`channel`只能读
   * 通过`FileOutputStream.getChannel()`获取的`channel`只能写
   * 通过`RandomAccessFile.getChannel()`是否能读写根据构造`RandomAccessFile`是的读写模式决定

2. **读取：**

   会从`channel`读取数据填充`ByteBuffer`，返回值标识督导了多少字节，-1标识到达了文件末尾

   ```java
   int readBytes = channel.read(buffer);
   ```

3. **写入：**

   在`while`循环中调用`channel.write()`是因为`write()`方法并不能保证一次将`buffer`中的内容全部写入到`channel`

   ```java
   while(buffer.hasRemainging()) {
       channel.write(buffer);
   }
   ```

4. **关闭：**

   `channel`必须关闭，不过调用了`FileInputStream`、`FileOutputStream`或者`RandomAccessFile`的`close()`方法会间接调用`channel.close()`方法

5. **位置：**

   获取当前位置

   ```java
   long pos = channel.position();
   ```

   设置当前位置

   ```java
   long newPos = ...;
   channel.position(newPos);
   ```

   设置当前位置时，如果设置为文件的末尾

   * 这时读取会返回 -1 
   * 这时写入，会追加内容，但要注意如果 position 超过了文件末尾，再写入时在新内容和原末尾之间会有空洞（00）

6. **大小：**

   使用`channel.size()`方法获取文件的大小

7. **强制写入：**

   操作系统处于性能的考虑，会将数据缓存，不是立刻写入磁盘。可以调用`channel.force(true)`方法将文件内容和元数据（文件的权限信息等）立刻写入磁盘

8. **transformTo()**&**transformFrom()：<u>FileChannelTest.test_transform()</u>**

   底层使用零拷贝，效率高

   * `transformFrom()`：目标端调用，从源端拉取数据
   * `transformTo()`：源端调用，向目标端传输数据

   ```java
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
   ```

### Path&Paths

> JDK 1.7引入了`Path`和`Paths`类，`Path`用来表示文件路径，`Paths`是工具类，用来获取`Path`实例

<u>**FileChannelTest#test_path**</u>

```java
Path path = Paths.get("src/test/resources/data.txt");
System.out.println(path);

Path resources = Paths.get("src/test", "resources");
System.out.println(resources);
```

### Files

> JDK 1.7引入了`Files`工具类，提供一些对文件的操作方法

#### 常用方法

**<u>FileChannelTest#test_file()</u>**

1. **检查文件是否存在：**

   ```java
   Path path = Paths.get("src/test/resources/data.txt");
   Files.exists(path)
   ```

2. **创建一级目录：**

   ```java
   path = Paths.get("src/test/resources/dir");
   Files.createDirectory(path);
   ```

3. **创建多级目录：**

   ```java
   path = Paths.get("src/test/resources/dir/d1/d2");
   Files.createDirectories(path);
   ```

4. **拷贝文件：**

   ```java
   Path source = Paths.get("src/test/resources/data.txt");
   Path target = Paths.get("src/test/resources/data_copy.txt");
   Files.copy(source, target);
   ```

5. **移动文件：**

   ```java
   source = Paths.get("src/test/resources/data_copy.txt");
   target = Paths.get("src/test/resources/data_move.txt");
   Files.move(source, target);
   ```

6. **删除文件：**

   ```java
   Path deletePath = Paths.get("src/test/resources/data_move.txt");
   Files.delete(deletePath);
   ```

7. **删除目录：**

   ```java
   Path deleteDir = Paths.get("src/test/resources/dir/d1/d2");
   Files.delete(deleteDir);
   ```

8. **遍历目录：<u>FileChannelTest#test_fileWalk()</u>**

   提供了`walkFileTree()`方法，使用访问者模式，对目录进行遍历，在使用时实现`SimpleFileVisitor`类中的相应方法，完成对目录的遍历：

   ```java
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
   ```

   
