package com.feizi.zipkin.trace.collector;

import com.github.kristofa.brave.SpanCollectorMetricsHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by feizi on 2018/4/12.
 */
public class SimpleMetricsHandler implements SpanCollectorMetricsHandler {
    private final AtomicInteger acceptedSpans = new AtomicInteger();
    private final AtomicInteger droppedSpans = new AtomicInteger();

    @Override
    public void incrementAcceptedSpans(int i) {
        acceptedSpans.addAndGet(i);
    }

    @Override
    public void incrementDroppedSpans(int i) {
        droppedSpans.addAndGet(i);
    }
}
