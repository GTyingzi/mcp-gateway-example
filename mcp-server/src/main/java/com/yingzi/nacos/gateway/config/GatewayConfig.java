package com.yingzi.nacos.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("nacos-route", r -> r.path("/gateway/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://mcp-restful-provider"))
                .build();
    }
}