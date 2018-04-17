package com.feizi.zipkin.trace;

import com.feizi.zipkin.trace.collector.SimpleMetricsHandler;
import com.github.kristofa.brave.AbstractSpanCollector;
import com.github.kristofa.brave.SpanCollectorMetricsHandler;
import com.feizi.zipkin.trace.collector.HttpCollector;
import com.feizi.zipkin.trace.context.TraceContext;
import com.twitter.zipkin.gen.Span;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 日志追踪代理器
 * Created by feizi on 2018/4/12.
 */
public class TraceAgent {
    private final AbstractSpanCollector collector;
    private final int THREAD_POOL_COUNT = 5;

    private final ExecutorService executor = Executors.newFixedThreadPool(this.THREAD_POOL_COUNT, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread worker = new Thread(r);
            //设置线程名称
            worker.setName("TRACE-AGENT-WORKER");
            /*设置线程守护*/
            worker.setDaemon(true);
            return worker;
        }
    });

    public TraceAgent(String serverUrl) {
        SpanCollectorMetricsHandler metrics = new SimpleMetricsHandler();
        collector = HttpCollector.create(serverUrl, TraceContext.getTraceConfig(), metrics);
    }

    public void send(final List<Span> spanList){
        if(null != spanList && !spanList.isEmpty()){
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    for (Span span :spanList){
                        collector.collect(span);
                    }
                    collector.flush();
                }
            });
        }
    }
}
