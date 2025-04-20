package org.springframework.ai.mcp.client.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpSseClientProperties;
import org.springframework.ai.mcp.client.autoconfigure.properties.McpClientCommonProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
    public List<NamedClientMcpTransport> webFluxClientTransports(McpSseClientProperties sseProperties, ObjectProvider<WebClient.Builder> webClientBuilderProvider, ObjectProvider<ObjectMapper> objectMapperProvider) {
        List<NamedClientMcpTransport> sseTransports = new ArrayList();
        WebClient.Builder webClientBuilderTemplate = (WebClient.Builder)webClientBuilderProvider.getIfAvailable(WebClient::builder);
        ObjectMapper objectMapper = (ObjectMapper)objectMapperProvider.getIfAvailable(ObjectMapper::new);
        Iterator var7 = sseProperties.getConnections().entrySet().iterator();

        while(var7.hasNext()) {
            Map.Entry<String, McpSseClientProperties.SseParameters> serverParameters = (Map.Entry)var7.next();
            WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(((McpSseClientProperties.SseParameters)serverParameters.getValue()).url())
                    .defaultHeaders((headers) -> {
                        serverParameters.getValue().headersMap().forEach(headers::add);
                    })
                    ;
            WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper);
            sseTransports.add(new NamedClientMcpTransport((String)serverParameters.getKey(), transport));
        }

        return sseTransports;
    }
}