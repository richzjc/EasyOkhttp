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
