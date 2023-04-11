# NIO（Non-blocking IO）
## ByteBuffer

> Buffer是非线程安全的

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
