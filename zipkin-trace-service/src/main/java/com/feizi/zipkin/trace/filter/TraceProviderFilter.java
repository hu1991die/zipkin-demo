package com.feizi.zipkin.trace.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.feizi.zipkin.trace.TraceAgent;
import com.feizi.zipkin.trace.context.TraceContext;
import com.feizi.zipkin.trace.utils.IdUtils;
import com.feizi.zipkin.trace.utils.NetworkUtils;
import com.google.common.base.Stopwatch;
import com.twitter.zipkin.gen.Annotation;
import com.twitter.zipkin.gen.Endpoint;
import com.twitter.zipkin.gen.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 提供端日志过滤器
 * Created by feizi on 2018/4/12.
 */
@Activate(group = {Constants.PROVIDER})
public class TraceProviderFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceProviderFilter.class);

    /**
     * 开始追踪
     * @param attaches
     * @return
     */
    private Span startTrace(Map<String, String> attaches){
        Long traceId = Long.valueOf(attaches.get(TraceContext.TRACE_ID_KEY));
        Long parentSpanId = Long.valueOf(attaches.get(TraceContext.SPAN_ID_KEY));

        TraceContext.start();
        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(parentSpanId);

        Span providerSpan = new Span();
        long id = IdUtils.get();
        providerSpan.setId(id);
        providerSpan.setParent_id(parentSpanId);
        providerSpan.setTrace_id(traceId);
        providerSpan.setName(TraceContext.getTraceConfig().getApplicationName());

        long timeStamp = System.currentTimeMillis() * 1000;
        providerSpan.setTimestamp(timeStamp);

        providerSpan.addToAnnotations(
                Annotation.create(timeStamp, TraceContext.ANNO_SR,
                        Endpoint.create(
                                TraceContext.getTraceConfig().getApplicationName(),
                                NetworkUtils.ip2Num(NetworkUtils.getSiteIp()),
                                TraceContext.getTraceConfig().getServerPort()
                        )
                )
        );
        TraceContext.addSpan(providerSpan);
        return providerSpan;
    }

    /**
     * 结束追踪
     * @param span
     * @param stopwatch
     */
    private void endTrace(Span span, Stopwatch stopwatch){
        span.addToAnnotations(
                Annotation.create(System.currentTimeMillis() * 1000, TraceContext.ANNO_SS,
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

        Map<String, String> attaches = invocation.getAttachments();
        if(!attaches.containsKey(TraceContext.TRACE_ID_KEY)){
            return invoker.invoke(invocation);
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        //开始追踪
        Span providerSpan = this.startTrace(attaches);

        Result result = invoker.invoke(invocation);
        /*结束追踪*/
        this.endTrace(providerSpan, stopwatch);
        return result;
    }
}
