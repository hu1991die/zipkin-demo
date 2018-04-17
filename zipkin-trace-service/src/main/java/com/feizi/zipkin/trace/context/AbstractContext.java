package com.feizi.zipkin.trace.context;

/**
 * 上下文基类
 * Created by feizi on 2018/4/12.
 */
public abstract class AbstractContext {
    private String applicationName;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }
}
