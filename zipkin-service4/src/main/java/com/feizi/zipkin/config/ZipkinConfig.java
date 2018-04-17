package com.feizi.zipkin.config;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.Brave.Builder;
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;
import com.github.kristofa.brave.Sampler;
import com.github.kristofa.brave.SpanCollector;
import com.github.kristofa.brave.http.DefaultSpanNameProvider;
import com.github.kristofa.brave.http.HttpSpanCollector;
import com.github.kristofa.brave.http.HttpSpanCollector.Config;
import com.github.kristofa.brave.httpclient.BraveHttpRequestInterceptor;
import com.github.kristofa.brave.httpclient.BraveHttpResponseInterceptor;
import com.github.kristofa.brave.servlet.BraveServletFilter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by feizi on 2018/4/11.
 */
@Configuration
public class ZipkinConfig {
    /**
     * 配置收集器
     * @return SpanCollector
     */
    @Bean
    public SpanCollector spanCollector(){
        Config config = Config.builder()
                .compressionEnabled(false)
                .connectTimeout(5000)
                .flushInterval(1)
                .readTimeout(6000)
                .build();
        return HttpSpanCollector.create("http://127.0.0.1:9411", config, new EmptySpanCollectorMetricsHandler());
    }

    /**
     * Brave各工具类的封装
     * @param spanCollector 收集器
     * @return Brave
     */
    @Bean
    public Brave brave(SpanCollector spanCollector){
        //指定ServiceName
        Builder builder = new Builder("Zipkin-Service4");
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
