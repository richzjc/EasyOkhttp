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

#### 1. 首先是创建OkhttpClient
   ```
    OkHttpClient client = new OkHttpClient.Builder().build();
   ```
   OkhttpClient是通过构造者设计模式创建的

   Builder里面的参数有许多， 重点列举如下几个，其它的我也不懂：
   ```
    Dispatcher dispatcher; // 调度器，在整个网络请求中拌演着重要的作用
    final List<Interceptor> interceptors = new ArrayList<>(); //拦截器，在执行网络之前执行
    final List<Interceptor> networkInterceptors = new ArrayList<>(); //网络请求的拦截器
    @Nullable SSLSocketFactory sslSocketFactory;
    ConnectionPool connectionPool;
    Dns dns;
    int connectTimeout;
    int readTimeout;
    int writeTimeout;
    int pingInterval;
   ```

   从过往的面试经历来看，经常会问到拦截器里面， `addInterceptor` 与 `addNetworkInterceptor`的区别， 你知道吗？
   
##### 2. 封装请求的Request
```
 Request request = new Request.Builder().url(PATH).build();
```
其实构建Request是比较简单的， 也是通过构造者模式构建的。主要参数有如下几个： 
```
    HttpUrl url; //请求地址 
    String method; //请求方式，是get请求还是post请求
    Headers.Builder headers; //请求头
    RequestBody body; //请求体
    Object tag;
```
##### 3. 创建RealCall
真正调用请求的代码是在RealCall里面执行的， RealCall是通过OkhttpClient与Request创建出来的， 代码如下： 
```
Call call = okHttpClient.newCall(request);

源码如下：
  @Override public Call newCall(Request request) {
    return RealCall.newRealCall(this, request, false);
  }
```

RealCall其内部持有属性有： 
```
final class RealCall implements Call {
  final OkHttpClient client; 
  final RetryAndFollowUpInterceptor retryAndFollowUpInterceptor;
  private EventListener eventListener;
  final Request originalRequest;
  final boolean forWebSocket; //标记是否是websocket
  private boolean executed; //标记当前请求是否已经执行
}
```
##### 4. 调用异步方法
构造出了RealCall对象， 这时可以调用RealCall的同步执行与异步执行的代码。
```
  @Override public void enqueue(Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    captureCallStackTrace();
    eventListener.callStart(this);
    client.dispatcher().enqueue(new AsyncCall(responseCallback));
  }
```
对于异步执行的代码，其实是通过OkhttpClient的调度器来实现的，内部通过线程池来实现的。
接下来分析一下，`client.dispatcher().enqueue(new AsyncCall(responseCallback));`
这句代码到底是做了哪些事情。

##### 5. Dispatcher的分析，包括线程池
首先看一下Dispacher里面的如下代码： 
```
  synchronized void enqueue(AsyncCall call) {
    if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
      runningAsyncCalls.add(call);
      executorService().execute(call);
    } else {
      readyAsyncCalls.add(call);
    }
  }
```
Dispatcher里面维护了3个双端队列， runningAsyncCalls表示正在执行的队列， readyAsyncCalls表示正在执行的队列。 runningSyncCalls表示同步执行的队列
代码如下：
```
  private final Deque<AsyncCall> readyAsyncCalls = new ArrayDeque<>();
  private final Deque<AsyncCall> runningAsyncCalls = new ArrayDeque<>();
  private final Deque<RealCall> runningSyncCalls = new ArrayDeque<>();
```
请求添加到队列后， 是通过线程池调用执行真正请求后端接口：
```
  public synchronized ExecutorService executorService() {
    if (executorService == null) {
      executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
          new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
    }
    return executorService;
  }
```
关于线程池，源码里面有涉及到最大请求数不能超过64， 也就是runningAsyncCalls的最大长度不能超过64。
另外就是单个域名的最大请求数不能超过5。 Dispatcher类里面明确声明：
```
private int maxRequests = 64;
private int maxRequestsPerHost = 5;
```

#### 6. Dispatcher处理之后，回调到RealCall
从之前的代码分析，调用Dispatcher的enqueue(AsyncCall call)方法，传入的是AsyncCall对象。而AsyncCall是实现了Runnable接口的。
因此得到结论，最终会执行AsyncCall的run()方法。并且AsyncCall作为RealCall的内部类，所以AsyncCall是持有RealCall的引用。
看一下真正run方法的代码, execute()方法是在父类的run()方法里面调用的：
```
    @Override protected void execute() {
      boolean signalledCallback = false;
      try {
        Response response = getResponseWithInterceptorChain();
        if (retryAndFollowUpInterceptor.isCanceled()) {
          signalledCallback = true;
          responseCallback.onFailure(RealCall.this, new IOException("Canceled"));
        } else {
          signalledCallback = true;
          responseCallback.onResponse(RealCall.this, response);
        }
      } catch (IOException e) {
        if (signalledCallback) {
          // Do not signal the callback twice!
          Platform.get().log(INFO, "Callback failure for " + toLoggableString(), e);
        } else {
          eventListener.callFailed(RealCall.this, e);
          responseCallback.onFailure(RealCall.this, e);
        }
      } finally {
        client.dispatcher().finished(this);
      }
    }
  }
```
从上面的代码能清楚的看到了` Response response = getResponseWithInterceptorChain();`
这句代码是通过真正的责任链模式去获取网络请求结果。

#### 7. 分析责任链模式
责任链的代码如下： 
```
  Response getResponseWithInterceptorChain() throws IOException {
    // Build a full stack of interceptors.
    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.addAll(client.interceptors());
    interceptors.add(retryAndFollowUpInterceptor);
    interceptors.add(new BridgeInterceptor(client.cookieJar()));
    interceptors.add(new CacheInterceptor(client.internalCache()));
    interceptors.add(new ConnectInterceptor(client));
    if (!forWebSocket) {
      interceptors.addAll(client.networkInterceptors());
    }
    interceptors.add(new CallServerInterceptor(forWebSocket));

    Interceptor.Chain chain = new RealInterceptorChain(interceptors, null, null, null, 0,
        originalRequest, this, eventListener, client.connectTimeoutMillis(),
        client.readTimeoutMillis(), client.writeTimeoutMillis());

    return chain.proceed(originalRequest);
  }
```
现在从上面的代码应该能分析出，addInterceptor()与addNetworkInterceptor的区别了吧，
因为这两者拦截器在责任链里面添加的位置都不同。

源码里面的拦截器就上面几种。抛开缓存，在学获取网络的数据是通过拦截器：CallServerInterceptor来实现的。
接下来分析一下CallServerInterceptor:

#### 8. CallServerInterceptor的实现
CallServerInterceptor是真正的获取网络流的逻辑。而底层的代码最终还是通过Java的socket类来实现的， 
只不过在Okhttp底层又做了一些处理，包括Http的1.0版本与Http2.0版本的判断逻辑。

从代码逻辑看应该是： 
`CallServerInterceptor -> HttpCodeC -> RealConnection`

RealConnection就会涉及到Socket的相关处理逻辑。

#### 9. 缓存拦截器

Okhttp里面的缓存拦截器，里面比较重要的一个类`CacheStrategy`,这里面涉及到Etag， Cache-control等相关的缓存处理。
源码如下： 
```
    public Factory(long nowMillis, Request request, Response cacheResponse) {
      this.nowMillis = nowMillis;
      this.request = request;
      this.cacheResponse = cacheResponse;

      if (cacheResponse != null) {
        this.sentRequestMillis = cacheResponse.sentRequestAtMillis();
        this.receivedResponseMillis = cacheResponse.receivedResponseAtMillis();
        Headers headers = cacheResponse.headers();
        for (int i = 0, size = headers.size(); i < size; i++) {
          String fieldName = headers.name(i);
          String value = headers.value(i);
          if ("Date".equalsIgnoreCase(fieldName)) {
            servedDate = HttpDate.parse(value);
            servedDateString = value;
          } else if ("Expires".equalsIgnoreCase(fieldName)) {
            expires = HttpDate.parse(value);
          } else if ("Last-Modified".equalsIgnoreCase(fieldName)) {
            lastModified = HttpDate.parse(value);
            lastModifiedString = value;
          } else if ("ETag".equalsIgnoreCase(fieldName)) {
            etag = value;
          } else if ("Age".equalsIgnoreCase(fieldName)) {
            ageSeconds = HttpHeaders.parseSeconds(value, -1);
          }
        }
      }
    }
```

## 总结

其实掌握了异步网络请求调用顺序后， 对于同步请求， websocket的原理，就比较好理解了。


   
