package com.yingzi.nacos.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author yingzi
 * @date 2025/4/20:00:01
 */
@Component
@ConfigurationProperties(prefix = "spring.cloud.nacos") // 绑定restful端服务
public class RestfulServicesConfig {
    private List<String> restfulServices;

    public List<String> getRestfulServices() {
        return restfulServices;
    }

    public void setRestfulServices(List<String> restfulServices) {
        this.restfulServices = restfulServices;
    }
}
