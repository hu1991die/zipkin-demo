package com.feizi.zipkin.trace.context;

import com.feizi.zipkin.trace.config.TraceConfig;
import com.twitter.zipkin.gen.Span;

import java.util.ArrayList;
import java.util.List;

/**
 * 调用链追踪上下文
 * Created by feizi on 2018/4/12.
 */
public class TraceContext extends AbstractContext {
    /*就是一个全局的跟踪ID，是跟踪的入口点，根据需求来决定在哪生成traceId, 比如一个http请求，首先入口是web应用，一般看完整的调用链这里自然是traceId生成的起点，结束点在web请求返回点。*/
    private static ThreadLocal<Long> TRACE_ID = new InheritableThreadLocal<>();
    /*这是下一层的请求跟踪ID,这个也根据自己的需求，比如认为一次rpc，一次sql执行等都可以是一个span。一个traceId包含一个以上的spanId。*/
    private static ThreadLocal<Long> SPAN_ID = new InheritableThreadLocal<>();
    /*SpanId请求追踪列表*/
    private static ThreadLocal<List<Span>> SPAN_LIST = new InheritableThreadLocal<>();

    public static final String TRACE_ID_KEY = "traceId";
    public static final String SPAN_ID_KEY = "spanId";

    /*客户端发起请求的时间，比如dubbo调用端开始执行远程调用之前。*/
    public static final String ANNO_CS = "cs";
    /*客户端收到处理完请求的时间。*/
    public static final String ANNO_CR = "cr";
    /*服务端处理完逻辑的时间。*/
    public static final String ANNO_SS = "ss";
    /*服务端收到调用端请求的时间。*/
    public static final String ANNO_SR = "sr";

    /* 调用链追踪配置类 */
    private static TraceConfig traceConfig;

    /**
     * 清空线程区
     */
    public static void clear(){
        TRACE_ID.remove();
        SPAN_ID.remove();
        SPAN_LIST.remove();
    }

    /**
     * 初始化
     * @param traceConfig
     */
    public static void init(TraceConfig traceConfig){
        setTraceConfig(traceConfig);
    }

    /**
     * 启动
     */
    public static void start(){
        clear();
        SPAN_LIST.set(new ArrayList<Span>());
    }

    public static TraceConfig getTraceConfig() {
        return traceConfig;
    }

    public static void setTraceConfig(TraceConfig traceConfig) {
        TraceContext.traceConfig = traceConfig;
    }

    private TraceContext() {
    }

    public static Long getTraceId() {
        return TRACE_ID.get();
    }

    public static void setTraceId(Long traceId) {
        TRACE_ID.set(traceId);
    }

    public static Long getSpanId() {
        return SPAN_ID.get();
    }

    public static void setSpanId(Long spanId) {
        SPAN_ID.set(spanId);
    }

    public static List<Span> getSpanList() {
        return SPAN_LIST.get();
    }

    public static void addSpan(Span span) {
        SPAN_LIST.get().add(span);
    }
}
