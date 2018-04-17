package com.feizi.zipkin.controller;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by feizi on 2018/4/11.
 */
@RestController
public class ZipkinController {

    @Autowired
    private CloseableHttpClient httpClient;

    @GetMapping("service1")
    public String service() throws Exception{
        System.out.println("=====进入service1");
        Thread.sleep(100);
        HttpGet get = new HttpGet("http://localhost:8082/service2");
        CloseableHttpResponse response = httpClient.execute(get);
        String result = EntityUtils.toString(response.getEntity(), "UTF-8");

        System.out.println("===result: " + result);
        return "Hello Service1, | " + result;
    }
}
