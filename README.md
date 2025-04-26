# 手撸轻量级Okhttp实现网络请求
---

### 远程仓库地址
mavenCentral()

---

### 添加依赖

api "io.github.richzjc:easyokhttp:1.0.0"

--- 

### 博客地址

https://www.baidu.com

---

### okhttp里面比较重要的几个类

`OkhttpClient`：通过 OkHttpClient 定义全局参数。

`Request`：Request 封装请求细节

`RealCall`：RealCall 触发执行

`Dispatcher`：Dispatcher 管理线程池和队列

`Interceptor`：Interceptor 链依次处理请求和响应（如缓存、压缩）。

`Cache`：Cache 和连接池优化性能

`Excutors`：Executors 确保线程高效复用

---
### 看一下Okhttp都会涉及到哪些面试题，带着问题来了解一下源码
1. OkHttp 拦截器链如何实现责任链模式？如何自定义拦截器实现请求耗时统计与敏感数据脱敏？
2. OkHttp 连接池如何实现 TCP 复用？如何优化高并发场景下的连接性能？
3. OkHttp 缓存机制如何扩展支持 POST 请求？如何实现动态缓存策略？
4. OkHttp 如何通过设计模式实现高扩展性？
5. OkHttp 用到了哪些设计模式
6. OkHttp 如何实现断点续传
7. OkHttp 的责任链模式都有哪些拦截器
8. OkHttp 的 CacheInterceptor是如何实现的
9. Okhttp 的调度器是如何实现的
10. Okhttp 的同步与异步请求的实现机制
11. Okhttp 的连接池与多路复用

---
### 分析源码是如何执行的

1. 首先是创建OkhttpClient
2. 封装请求的Request
3. 创建RealCall
4. 调用异步方法
5. 调用同步方法
6. 调用责任链
7. 分析缓存的逻辑
8. 分析底层的网络请求
   
