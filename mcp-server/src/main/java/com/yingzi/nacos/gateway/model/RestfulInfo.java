package com.yingzi.nacos.gateway.model;

import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/6:15:51
 */
public class RestfulInfo {
    // 接口方法名称
    String methodName;
    // 接口地址
    String path;
    // 接口入参
    List<Parameter> parameters;
    public RestfulInfo(String methodName, String path, List<Parameter> parameters) {
        this.methodName = methodName;
        this.path = path;
        this.parameters = parameters;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }
}
