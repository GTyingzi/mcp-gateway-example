package com.yingzi.nacos.gateway.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${webclient.max-connections:500}")
    private int maxConnections;

    @Value("${webclient.acquire-timeout:3000}")
    private int acquireTimeout;

    @Value("${webclient.connection-timeout:3000}")
    private int connectionTimeout;

    @Value("${webclient.read-timeout:5000}")
    private int readTimeout;

    @Value("${webclient.write-timeout:5000}")
    private int writeTimeout;

    @Value("${webclient.max-idle-time:20}")
    private int maxIdleTime;

    @Value("${webclient.max-life-time:60}")
    private int maxLifeTime;

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> 
                values.forEach(value -> log.debug("Request Header: {}={}", name, value))
            );
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status: {}", clientResponse.statusCode());
            clientResponse.headers().asHttpHeaders().forEach((name, values) -> 
                values.forEach(value -> log.debug("Response Header: {}={}", name, value))
            );
            return Mono.just(clientResponse);
        });
    }

    @Bean
    public WebClient webClient() {
        // 配置连接池
        ConnectionProvider provider = ConnectionProvider.builder("http-pool")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(acquireTimeout))
                .maxIdleTime(Duration.ofSeconds(maxIdleTime))
                .maxLifeTime(Duration.ofSeconds(maxLifeTime))
                .build();

        // 配置 HTTP 客户端
        HttpClient httpClient = HttpClient.create(provider)
                // TCP 连接超时
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                // 响应超时
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn
                        // 读取超时
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        // 写入超时
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        // 配置连接池
        ConnectionProvider provider = ConnectionProvider.builder("http-pool")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(acquireTimeout))
                .maxIdleTime(Duration.ofSeconds(maxIdleTime))
                .maxLifeTime(Duration.ofSeconds(maxLifeTime))
                .build();

        // 配置 HTTP 客户端
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse());
    }
}
