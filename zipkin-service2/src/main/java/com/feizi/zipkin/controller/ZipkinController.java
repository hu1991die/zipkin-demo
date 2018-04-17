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

    @GetMapping("service2")
    public String service() throws Exception{
        System.out.println("=====进入service2");
        Thread.sleep(100);
        HttpGet get = new HttpGet("http://localhost:8083/service3");

        /*service2调用service3*/
        CloseableHttpResponse response = httpClient.execute(get);
        String result1 = EntityUtils.toString(response.getEntity(), "UTF-8");
        System.out.println("===result1: " + result1);

        /*service2调用service4*/
        get = new HttpGet("http://localhost:8084/service4");
        response = httpClient.execute(get);
        String result2 = EntityUtils.toString(response.getEntity(), "UTF-8");
        System.out.println("===result2: " + result2);


        return "Hello Service2, | " + result1 + ", | " + result2;
    }
}
