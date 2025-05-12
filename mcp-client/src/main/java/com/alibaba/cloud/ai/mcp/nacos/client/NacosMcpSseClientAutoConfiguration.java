package com.alibaba.cloud.ai.mcp.nacos.client;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpRegistryProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.config.NacosConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.client.autoconfigure.NamedClientMcpTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * @author yingzi
 * @date 2025/4/29:17:04
 */
@AutoConfiguration
@ConditionalOnClass({WebFluxSseClientTransport.class})
@EnableConfigurationProperties({ NacosMcpSseClientProperties.class, NacosMcpRegistryProperties.class})
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.client",
        name = {"enabled"},
        havingValue = "true",
        matchIfMissing = true
)
public class NacosMcpSseClientAutoConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(NacosMcpSseClientAutoConfiguration.class);

    public NacosMcpSseClientAutoConfiguration() {
    }

    @Bean
    public NamingService namingService(NacosMcpRegistryProperties nacosMcpRegistryProperties) {
        Properties nacosProperties = nacosMcpRegistryProperties.getNacosProperties();
        try {
            return NamingFactory.createNamingService(nacosProperties);
        }
        catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public NacosConfigService  nacosConfigService(NacosMcpRegistryProperties nacosMcpRegistryProperties) {
        Properties nacosProperties = nacosMcpRegistryProperties.getNacosProperties();
        try {
            return new NacosConfigService(nacosProperties);
        }
        catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean(name = "server2NamedTransport")
    public Map<String, List<NamedClientMcpTransport>> server2NamedTransport(
            NacosMcpSseClientProperties nacosMcpSseClientProperties,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
            NamingService namingService,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        Map<String, List<NamedClientMcpTransport>> server2NamedTransport = new HashMap<>();
        WebClient.Builder webClientBuilderTemplate = (WebClient.Builder) webClientBuilderProvider
                .getIfAvailable(WebClient::builder);
        ObjectMapper objectMapper = (ObjectMapper) objectMapperProvider.getIfAvailable(ObjectMapper::new);

        Map<String, String> connections = nacosMcpSseClientProperties.getConnections();
        connections.forEach((serviceKey, service) -> {
            try {
                List<Instance> instances = namingService.selectInstances(service + "-mcp-service", "mcp-server",true);
                List<NamedClientMcpTransport> namedTransports = new ArrayList<>();
                for (Instance instance : instances) {
                    String url = instance.getMetadata().getOrDefault("scheme", "http") + "://" + instance.getIp() + ":"
                            + instance.getPort();

                    WebClient.Builder webClientBuilder = webClientBuilderTemplate.clone().baseUrl(url);
                    WebFluxSseClientTransport transport = new WebFluxSseClientTransport(webClientBuilder, objectMapper);
                    namedTransports.add(
                            new NamedClientMcpTransport(service + "-" + instance.getInstanceId(), transport));
                }
                if  (namedTransports.isEmpty()) {
                    throw new RuntimeException("No instances found for service: " + service);
                }
                server2NamedTransport.put(service, namedTransports);
            }
            catch (NacosException e) {
                logger.error("nacos naming service: {} error", service, e);
            }
        });
        return server2NamedTransport;
    }

}
