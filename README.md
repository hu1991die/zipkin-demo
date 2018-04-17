# zipkin-demo
## ZIPKIN分布式系统调用链追踪
> 在公司业务发展过程中，刚开始的时候，我们可能比较关注单个请求的调用耗时情况，调用频次统计等一些基本数据指标，因为这个时候业务比较单一，系统相对来说较为简单清晰，调整和优化起来相对来说比较容易一点。

> 但是随着系统业务的不断发展，需求的不断增加，整个系统逐渐变得越来越复杂，有可能还会涉及到外部系统以及公司内部其他系统之间的一个交互。这个时候整个系统的调用链将会变得越来复杂（目前大多数都是分布式调用），更多的时候我们的一个前端请求可能最终需要经过多次后端服务的调用才能得到我们想要的结果数据，而在这个过程中，如果调用超时或者调用变得异常的慢或者调用出现异常等等情况，一般情况下我们是无法准确快速得定位出这次请求出现的问题具体是因为那一部分出现了状况引起的。

> 比如说具体是调用服务A出现了问题，还是服务B不可用亦或者是服务C调用超时等等我们是无法得知的。那这个时候就需要对整个调用链做一次完整的分析才能快速准确的定位出具体的问题，因而分布式调用追踪就诞生了。

## 什么是分布式系统调用追踪？
1.	对多个相互协作的子系统之间的调用关系及依赖关系进行追踪记录
2.	系统与系统之间的调用形式有多种：HTTP、RPC、RMI等等
3.	通过调用链的方式，将一次请求调用过程完整的串联起来，这样就实现了对请求调用路径的监控

## 为什么需要进行分布式调用追踪？
1.	确定服务与服务之间的依赖关系，以便后期优化
2. 统计请求总耗时，以及各个服务的调用耗时，以便解决系统瓶颈
3. 当请求变慢或系统出现异常时，需要尽快确定具体是哪个服务出现问题，以便快速排查线上问题

## 分布式调用追踪需要做什么？
1. 记录从上游到下游关键节点服务的日志信息，入参、出参、异常堆栈等信息
2. 关键节点服务的响应耗时
3. 关键节点服务之间的依赖关系
4. 将分散的请求串联到一起

## 几个关键概念
### traceId：
标识整个请求链，就是一个全局的跟踪ID，是跟踪的入口点，根据需求来决定在哪生成traceId。比如一个http请求，首先入口是web应用，一般看完整的调用链这里自然是traceId生成的起点，结束点在web请求返回点，因此traceId将在这个调用链中进行传递。

### spanId：
标识一次分布式调用，这是下一层的请求跟踪ID,这个也根据自己的需求，比如认为一次rpc，一次sql执行等都可以是一个span。一个traceId包含一个以上的spanId。

### parentId:
上一次请求跟踪ID，用来将前后的请求串联起来。

### cs:
客户端发起请求的时间，比如dubbo调用端开始执行远程调用之前,标志着Span的开始

### cr:
客户端收到处理完请求的时间,标志着Span的结束

### ss:
服务端处理完逻辑的时间

### sr：
服务端收到调用端请求的时间

```
客户端调用时间 = cr-cs
服务端处理时间 = sr-ss 
```

```
sr-cs：表示网络延迟和时钟抖动
ss-sr：表示服务端处理请求耗时
cr-ss：表示网络延迟和时钟抖动
```

## Zipkin概述
> Zipkin官网：[https://zipkin.io/](https://zipkin.io/)。

> 各个业务系统在相互协作调用时，将特定的跟踪消息传递至zipkin，zipkin在收集到跟踪消息之后将其进行聚合处理，存储，展示等，用户可通过web UI方便清晰获得网络延迟、调用链路、系统依赖等。

### Zipkin主要涉及到四大组件
1. Collector收集器：负责接收各service传输的数据
2. Cassandra存储器：作为Storage的一种，也可以是mysql等，默认存储在内存里面
3. Query查询器：负责查询Storage中存储的数据信息，并且提供简单的JSON API接口获取数据给WEB UI使用
4. Web UI展示器：提供简单的web可视化界面

### 安装
1. 执行以下命令下载jar包
```
wget -O zipkin.jar 'https://search.maven.org/remote_content?g=io.zipkin.java&a=zipkin-server&v=LATEST&c=exec'
```

2. 由于本身是一个spring boot项目，所以可以直接运行jar
```nohup java -jar zipkin.jar &```

3. 浏览器访问：[http://127.0.0.1:9411](http://127.0.0.1:9411)


### 简单demo
1. 新建四个工程项目：service1，service2，service3，service4

2. 它们之间的依赖关系如下：
service1调用service2，service2调用service3和service4，service3和service4互相没有依赖，直接返回。

3. 新建4个springboot项目：

4. pom.xml文件引入zipkin的相关坐标依赖：
```
<dependency>
   <groupId>io.zipkin.brave</groupId>
   <artifactId>brave-core</artifactId>
   <version>3.9.0</version>
</dependency>

<dependency>
   <groupId>io.zipkin.brave</groupId>
   <artifactId>brave-spancollector-http</artifactId>
   <version>3.9.0</version>
</dependency>

<dependency>
   <groupId>io.zipkin.brave</groupId>
   <artifactId>brave-web-servlet-filter</artifactId>
   <version>3.9.0</version>
</dependency>

<dependency>
   <groupId>io.zipkin.brave</groupId>
   <artifactId>brave-apache-http-interceptors</artifactId>
   <version>3.9.0</version>
</dependency>

<dependency>
   <groupId>org.apache.httpcomponents</groupId>
   <artifactId>httpclient</artifactId>
</dependency>

```

5. 编写zipkin配置类
```
@Configuration
public class ZipkinConfig {
    /**
     * 配置收集器
     * @return SpanCollector
     */
    @Bean
    public SpanCollector spanCollector(){
        Config config = HttpSpanCollector.Config.builder()
                .compressionEnabled(false)
                .connectTimeout(5000)
                .flushInterval(1)
                .readTimeout(6000)
                .build();
        return HttpSpanCollector.create("http://10.0.4.62:9411", config, new EmptySpanCollectorMetricsHandler());
    }

    /**
     * Brave各工具类的封装
     * @param spanCollector 收集器
     * @return Brave
     */
    @Bean
    public Brave brave(SpanCollector spanCollector){
        //指定ServiceName
        Builder builder = new Builder("Zipkin-Service1");
        builder.spanCollector(spanCollector);
        //设置采集率
        builder.traceSampler(Sampler.create(1));
        return builder.build();
    }

    /**
     * 拦截器，需要serverRequestInterceptor,serverResponseInterceptor 分别完成sr和ss操作
     * @param brave
     * @return
     */
    @Bean
    public BraveServletFilter braveServletFilter(Brave brave){
        return new BraveServletFilter(brave.serverRequestInterceptor(),
                brave.serverResponseInterceptor(), new DefaultSpanNameProvider());
    }

    /**
     * httpClient客户端，需要clientRequestInterceptor,clientResponseInterceptor分别完成cs和cr操作
     * @param brave
     * @return
     */
    @Bean
    public CloseableHttpClient closeableHttpClient(Brave brave){
        CloseableHttpClient httpClient = HttpClients.custom()
                .addInterceptorFirst(new BraveHttpRequestInterceptor(brave.clientRequestInterceptor(),
                        new DefaultSpanNameProvider()))
                .addInterceptorFirst(new BraveHttpResponseInterceptor(brave.clientResponseInterceptor()))
                .build();
        return httpClient;
    }
}
```

#### SpanCollector：
配置收集器。

#### Brave：
各工具类的封装,其中builder.traceSampler(Sampler.ALWAYS_SAMPLE)设置采样比率，0-1之间的百分比。

#### BraveServletFilter：
作为拦截器，需要serverRequestInterceptor,serverResponseInterceptor 分别完成sr和ss操作

#### CloseableHttpClient：
添加拦截器，需要clientRequestInterceptor,clientResponseInterceptor 分别完成cs和cr操作,该功能由brave中的brave-okhttp模块提供，同样的道理如果需要记录数据库的延迟只要在数据库操作前后完成cs和cr即可，当然brave提供其封装。

6. 编写各个项目的controller

### 注意事项：
1. service1、service2、service3和service4的服务端口号需要修改成不一样（例如：8081,8082,8083,8084），不然启动会报错。

2. ZipkinConfig配置类指定的应用服务名称需要改变

### 测试分布式追踪
1. 分别启动四个服务。启动完成之后，浏览器访问：http://localhost:8081/service1

2. 在Web UI界面即可看到调用链以及依赖关系：

## Zipkin + Dubbo整合
### 基本原理：
通过编写filter过滤器，在请求处理的前后发送日志数据，让zipkin生成调用链数据。单独抽离出一个项目，通过注解配置的方式实现调用链的追踪。

1. 编写自动配置的注解类
2. 编写自动配置的实现，主要是将特定配置节点的值读取到上下文对象中
3. 编写调用追踪配置类，主要用于配置追踪的一些参数，zipkin地址
4. 配置Spring自动加载
```
a. 在resources/META-INF目录下创建spring.factories文件

b. spring.factories内容为：
org.springframework.boot.autoconfigure.EnableAutoConfiguration=com.touna.loan.trace.config.EnableTraceAutoConfiguration
```

5. 追踪上下文
6. 创建Zipkin日志收集器
```
使用http的方式将日志信息发送给zipkin服务，中间可以配合压缩等优化手法
```

7. 日志收集器代理
```
使用线程池异步执行日志发送，避免阻塞正常业务逻辑
```

### Dubbo Fillter过滤器
#### 消费端Filter
消费端作为调用链的入口，需要判断是首次调用，还是内部多次调用。如果是首次调用则生成新的traceId和spanId,如果是内部多次调用，那么就直接从TraceContext调用链上下文中获取traceId和spanId。消费端需要通过Invocation的参数列表将生成的traceId和spanId传递到下游系统中。

#### 服务端Filter
基本与消费端的逻辑类似，只是这里将服务端的日志信息发送给zipkin，服务端接收消费端从Invocation的参数列表中传递过来的traceId和spanId，从而将整个RPC的调用逻辑串联起来。

### Filter应用
#### 消费端：
##### 1.Application启动类开启自动追踪注解```@EnableTraceAutoConfigurationProperties```

##### 2.配置消费端过滤器
```
<dubbo:consumer filter="traceConsumerFilter"/>
```
#### 服务端：
##### 1.Application启动类启用自动追踪注解```@EnableTraceAutoConfigurationProperties```

##### 2.配置服务端过滤器
```
<dubbo:provider filter="traceProviderFilter" />
```

#### 原文参考：
1. [https://www.cnblogs.com/ASPNET2008/p/6709900.html](https://www.cnblogs.com/ASPNET2008/p/6709900.html)
2. [https://t.hao0.me/devops/2016/10/15/distributed-invoke-trace.html](https://t.hao0.me/devops/2016/10/15/distributed-invoke-trace.html)
3. [https://www.cnblogs.com/binyue/p/5703812.html](https://www.cnblogs.com/binyue/p/5703812.html)


