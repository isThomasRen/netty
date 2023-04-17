# NIO（Non-blocking IO）
## ByteBuffer

> `Buffer`是非线程安全的

相关代码见：<u>cn.thomas.netty.chapter01.Code01_ByteBufferTest</u>

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

相关代码见：<u>cn.thomas.netty.chapter01.Code02_FileTest</u>

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


## 网络编程

### 阻塞IO

**<u>cn.thomas.netty.chapter01.Code03_NIOTest#test_blockingNIOServer()</u>**

**服务器端：**

1. 开启`serverSocketChannel`管道
2. 对`serverSocketChannel`绑定监听端口
3. `serverSocketChannel`阻塞式等待连接
4. 从建立好的`socketChannel`阻塞式读取通道中数据

```java
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
```

**客户端：**

1. 开启`SocketChannel`管道
2. 对`SocketChannel`绑定连接的服务器地址及端口号

```java
SocketChannel socketChannel = SocketChannel.open();
socketChannel.connect(new InetSocketAddress("localhost", 8080));
System.out.println("debug...");
```

**阻塞模式存在的问题：**

`serverSocketChannel.accept()`方法和`socketChannel.read()`方法均会导致线程阻塞，无法同时处理多个客户端的连接通信，并且效率较低

### 非阻塞IO

1. 开启`serverSocketChannel`管道
2. 对`serverSocketChannel`绑定监听端口
3. 将`serverSocketChannel`设置为非阻塞模式
4. `serverSocketChannel`阻塞式等待连接
5. 将`socketChannel`设置为非阻塞模式
6. 从建立好的`socketChannel`阻塞式读取通道中数据

**<u>cn.thomas.netty.chapter01.Code03_NIOTest#test_nonblockingNioServer()</u>**

**服务器端：**

```java
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
```

**非阻塞模式存在的问题：**

解决了阻塞式IO中服务器端一个线程只能同时处理一个客户端的读写请求的问题，但无论是否存在连接、读写时间的发生，循环一直处于运行状态，消耗CPU资源

### 多路复用

#### Selector

* **创建：**

  ```java
  Selector selector = Selector.open();
  ```

* **绑定Channel事件：**

  也称为注册事件，绑定的事件`selector`才会关心

  ```java
  channel.configureBlocking(false);
  SelectionKey key = channel.register(selector, 绑定事件);
  ```

  * `channel`必须处于非阻塞模式
  * `FileChannel`没有非阻塞模式，因此不能配合`selector`一起使用
  * 绑定的事件类型有：
    * `OP_ACCEPT`：服务器端成功接受连接时触发
    * `OP_CONNECT`：客户端连接成功时触发
    * `OP_READ`：数据可读入时触发，有因为接收能力弱，数据暂不能读入的情况
    * `OP_WRITE`：数据可写出时触发，有因为发送能力弱，数据暂不能写出的情况

* **监听Channel事件：**

  可通过以下三种方式来监听是否有事件发生，返回的方法值代表有多少`channel`发生了事件：

  方法1：阻塞直到绑定事件发生

  ```java
  int count = selector.select();
  ```

  方法2：阻塞直到绑定事件发生，或者超时（单位ms）

  ```java
  int count = selector.select(long timeout);
  ```

  方法3：不会阻塞，也就是不管有没有事件，立刻返回，自己根据返回值检查是否有事件

  ```java
  int count = selector.selectNow();
  ```

#### 处理accept事件

**<u>cn.thomas.netty.chapter01.Code04_SelectorTest#test_serverWithSelector()</u>**

> 事件发生后，要么处理，要么取消（`cancel`），不能什么都不做，否则下次该事件仍会触发

```java
if (selectionKey.isAcceptable()) {
    ServerSocketChannel ssc = (ServerSocketChannel) selectionKey.channel();
    SocketChannel socketChannel = ssc.accept();
    socketChannel.configureBlocking(false);
    log.debug("获取到客户端连接：{}", socketChannel);
    // 注册到选择器上，监听读事件，并绑定一个ByteBuffer作为附件
    SelectionKey key = socketChannel.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(4));
    key.interestOps(SelectionKey.OP_READ);
}
```

#### 处理read事件

**<u>cn.thomas.netty.chapter01.Code04_SelectorTest#test_serverWithSelector()</u>**

* `socketChannel.read()`在当客户端强制断开连接时会抛出异常，为避免服务器因异常退出，需要对异常进行捕获，处理相关逻辑
* 在当客户端正常退出时，服务器会通过`socketChannel.read()`接收到长度为-1的读事件，需要对相关逻辑进行处理

```java
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
```

```java
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
```

#### 处理write事件

**<u>cn.thomas.netty.chapter01.Code05_WriteTest#test_writeServer()</u>**

> 只要向 channel 发送数据时，socket 缓冲可写，这个事件会频繁触发，因此应当只在 socket 缓冲区写不下时再关注可写事件，数据写完之后再取消关注

```java
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
```

### 多线程优化

**<u>cn.thomas.netty.chapter01.Code06_MultiThreadTest</u>**

**服务器端：**

```java
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
```

```java
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
        // 唤醒selector
        worker.wakeup();
    }

    @Override
    public void run() {
        log.debug("worker线程启动...");
        while (true) {
            try {
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
```

