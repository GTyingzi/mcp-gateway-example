package com.yingzi.nacos.gateway;

import com.yingzi.nacos.gateway.component.RestfulToolComponent;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransport;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author yingzi
 * @date 2025/4/5:19:04
 */
@SpringBootApplication
@EnableDiscoveryClient
@LoadBalancerClients({
        @LoadBalancerClient("mcp-restful-provider")
})
@ComponentScan(basePackages = {"com.yingzi.nacos.gateway", "io.modelcontextprotocol.server.transport"})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(RestfulToolComponent restfulToolComponent) {
        return restfulToolComponent.parseRestfulInfo();
    }
}