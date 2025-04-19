package com.yingzi.nacos.gateway;

import com.yingzi.nacos.gateway.component.RestfulToolComponent;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * @author yingzi
 * @date 2025/4/5:19:04
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewaySecondApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewaySecondApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(RestfulToolComponent restfulToolComponent) {
        return restfulToolComponent.parseRestfulInfo();
    }
}