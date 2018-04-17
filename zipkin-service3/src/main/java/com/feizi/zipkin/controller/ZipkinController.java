package com.feizi.zipkin.controller;

import org.apache.http.impl.client.CloseableHttpClient;
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

    @GetMapping("service3")
    public String service() throws Exception{
        System.out.println("=====进入service3");
        return "Hello Service3";
    }
}
