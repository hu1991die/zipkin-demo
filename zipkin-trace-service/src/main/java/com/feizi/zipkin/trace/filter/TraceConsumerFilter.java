package com.feizi.zipkin.trace.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.feizi.zipkin.trace.utils.NetworkUtils;
import com.google.common.base.Stopwatch;
import com.feizi.zipkin.trace.TraceAgent;
import com.feizi.zipkin.trace.context.TraceContext;
import com.feizi.zipkin.trace.utils.IdUtils;
import com.twitter.zipkin.gen.Annotation;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 消费端日志过滤器
 * Created by feizi on 2018/4/12.
 */
@Activate(group = {Constants.CONSUMER})
public class TraceConsumerFilter implements Filter{
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceConsumerFilter.class);

    /**
     * 开始追踪
     * @param invoker
     * @param invocation
     * @return
     */
    private Span startTrace(Invoker<?> invoker, Invocation invocation){
        Span consumerSpan = new Span();

        Long traceId;
        long id = IdUtils.get();
        consumerSpan.setId(id);
        if(null == TraceContext.getTraceId()){
            TraceContext.start();
            traceId = id;
        }else {
            traceId = TraceContext.getTraceId();
        }

        /*全局追踪ID*/
        consumerSpan.setTrace_id(traceId);
        /*每一层级的父ID*/
        consumerSpan.setParent_id(TraceContext.getSpanId());
        /*设置追踪的应用名称*/
        consumerSpan.setName(TraceContext.getTraceConfig().getApplicationName());

        long timeStamp = System.currentTimeMillis() * 1000;
        consumerSpan.setTimestamp(timeStamp);

        consumerSpan.addToAnnotations(
                //cs:客户端发起请求的时间
                Annotation.create(timeStamp, TraceContext.ANNO_CS,
                        Endpoint.create(
                                TraceContext.getTraceConfig().getApplicationName(),
                                NetworkUtils.ip2Num(NetworkUtils.getSiteIp()),
                                TraceContext.getTraceConfig().getServerPort()
                        )
                )
        );

        Map<String, String> attaches = invocation.getAttachments();
        attaches.put(TraceContext.TRACE_ID_KEY, String.valueOf(consumerSpan.getTrace_id()));
        attaches.put(TraceContext.SPAN_ID_KEY, String.valueOf(consumerSpan.getId()));
        return consumerSpan;
    }

    /**
     * 结束追踪
     * @param span
     * @param stopwatch
     */
    private void endTrace(Span span, Stopwatch stopwatch){
        span.addToAnnotations(
              //cr:客户端收到处理完请求的时间
              Annotation.create(System.currentTimeMillis() * 1000, TraceContext.ANNO_CR,
                      Endpoint.create(
                              span.getName(),
                              NetworkUtils.ip2Num(NetworkUtils.getSiteIp()),
                              TraceContext.getTraceConfig().getServerPort()
                      )
              )
        );

        span.setDuration(stopwatch.stop().elapsed(TimeUnit.MICROSECONDS));
        TraceAgent traceAgent = new TraceAgent(TraceContext.getTraceConfig().getZipkinUrl());
        traceAgent.send(TraceContext.getSpanList());
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if(!TraceContext.getTraceConfig().isEnabled()){
            return invoker.invoke(invocation);
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        /*开始调用追踪*/
        Span span = this.startTrace(invoker, invocation);
        TraceContext.start();
        TraceContext.setTraceId(span.getTrace_id());
        TraceContext.setSpanId(span.getId());
        TraceContext.addSpan(span);

        Result result = invoker.invoke(invocation);
        /*结束调用追踪*/
        this.endTrace(span, stopwatch);

        return result;
    }
}
