package com.yingzi.nacos.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author yingzi
 * @date 2025/4/5:19:04
 */
@SpringBootApplication
@ComponentScan(basePackages = {"org.springframework.ai.mcp.server.autoconfigure", "com.yingzi.nacos.gateway"})
public class ServerWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerWebfluxApplication.class, args);
    }

}