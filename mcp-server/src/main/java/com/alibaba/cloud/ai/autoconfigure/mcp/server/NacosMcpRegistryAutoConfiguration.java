package com.alibaba.cloud.ai.autoconfigure.mcp.server;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpProperties;
import com.alibaba.cloud.ai.mcp.nacos.registry.NacosMcpRegister;
import com.alibaba.cloud.ai.mcp.nacos.registry.NacosMcpRegistryProperties;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.springframework.ai.mcp.server.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.autoconfigure.McpServerProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author yingzi
 * @date 2025/5/21 21:28
 */

@EnableConfigurationProperties({ NacosMcpRegistryProperties.class, NacosMcpProperties.class,
        McpServerProperties.class })
@AutoConfiguration(after = McpServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = McpServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class NacosMcpRegistryAutoConfiguration {

    @Bean
    @ConditionalOnBean(McpSyncServer.class)
    @ConditionalOnProperty(prefix = NacosMcpRegistryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = false)
    public NacosMcpRegister nacosMcpRegisterSync(McpSyncServer mcpSyncServer, NacosMcpProperties nacosMcpProperties,
                                                 NacosMcpRegistryProperties nacosMcpRegistryProperties, McpServerTransportProvider mcpServerTransport) {
        McpAsyncServer mcpAsyncServer = mcpSyncServer.getAsyncServer();
        return getNacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, mcpServerTransport);
    }

    @Bean
    @ConditionalOnBean(McpAsyncServer.class)
    @ConditionalOnProperty(prefix = NacosMcpRegistryProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
            matchIfMissing = false)
    public NacosMcpRegister nacosMcpRegisterAsync(McpAsyncServer mcpAsyncServer, NacosMcpProperties nacosMcpProperties,
                                                  NacosMcpRegistryProperties nacosMcpRegistryProperties, McpServerTransportProvider mcpServerTransport) {
        return getNacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, mcpServerTransport);
    }

    private NacosMcpRegister getNacosMcpRegister(McpAsyncServer mcpAsyncServer, NacosMcpProperties nacosMcpProperties,
                                                 NacosMcpRegistryProperties nacosMcpRegistryProperties, McpServerTransportProvider mcpServerTransport) {
        if (mcpServerTransport instanceof StdioServerTransportProvider) {
            return new NacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, "stdio");
        }
        else {
            return new NacosMcpRegister(mcpAsyncServer, nacosMcpProperties, nacosMcpRegistryProperties, "sse");
        }
    }

}