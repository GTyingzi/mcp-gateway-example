package org.springframework.ai.autoconfigure.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.ai.autoconfigure.mcp.client.properties.McpClientCommonProperties;
import org.springframework.ai.autoconfigure.mcp.client.properties.McpSseClientProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author yingzi
 * @date 2025/4/8:13:24
 */
@AutoConfiguration
@ConditionalOnClass({WebFluxSseClientTransport.class})
@EnableConfigurationProperties({McpSseClientProperties.class, McpClientCommonProperties.class})
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.client",
        name = {"enabled"},
        havingValue = "true",
        matchIfMissing = true
)
public class SseWebFluxTransportAutoConfiguration {
    public SseWebFluxTransportAutoConfiguration() {
    }

    @Bean
    public List<NamedClientMcpTransport> webFluxClientTransports(McpSseClientProperties sseProperties, WebClient.Builder webClientBuilderTemplate, ObjectMapper objectMapper) {
        List<NamedClientMcpTransport> sseTransports = new ArrayList();
        Iterator var5 = sseProperties.getConnections().entrySet().iterator();
        while(var5.hasNext()) {
            Map.Entry<String, McpSseClientProperties.SseParameters> serverParameters = (Map.Entry)var5.next();
            WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone()
                    .baseUrl(serverParameters.getValue().url())
                    .defaultHeaders((headers) -> {
                        serverParameters.getValue().headersMap().forEach(headers::add);
                    });
            WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper);
            sseTransports.add(new NamedClientMcpTransport((String)serverParameters.getKey(), transport));
        }

        return sseTransports;
    }

    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}