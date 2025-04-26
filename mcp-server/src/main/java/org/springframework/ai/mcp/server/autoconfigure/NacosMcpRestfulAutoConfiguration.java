package org.springframework.ai.mcp.server.autoconfigure;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceManager;
import com.yingzi.nacos.gateway.component.DynamicRestfulToolsWatch;
import com.yingzi.nacos.gateway.component.InitRestfulToolComponent;
import com.yingzi.nacos.gateway.config.RestfulServicesConfig;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.DynamicMcpSyncToolsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.log.LogAccessor;

/**
 * @author yingzi
 * @date 2025/4/24:13:35
 */
@AutoConfiguration(after = NacosServiceManager.class)
@EnableConfigurationProperties({RestfulServicesConfig.class})
public class NacosMcpRestfulAutoConfiguration {

    private static final LogAccessor logger = new LogAccessor(McpServerAutoConfiguration.class);

    public NacosMcpRestfulAutoConfiguration() {
    }
    @Bean
    public DynamicRestfulToolsWatch dynamicRestfulToolsWatch(NacosServiceManager nacosServiceManager, DynamicMcpSyncToolsProvider dynamicMcpSyncToolsProvider, NacosDiscoveryProperties nacosDiscoveryProperties) {
        logger.info("Starting DynamicRestfulToolsWatch");
        nacosServiceManager.setNacosDiscoveryProperties(nacosDiscoveryProperties);
        return new DynamicRestfulToolsWatch(nacosServiceManager.getNamingService(), dynamicMcpSyncToolsProvider);
    }

    @Bean
    public DynamicMcpSyncToolsProvider dynamicMcpSyncToolsProvider(McpSyncServer mcpSyncServer) {
        logger.info("Starting DynamicMcpSyncToolsProvider");
        return new DynamicMcpSyncToolsProvider(mcpSyncServer);
    }

    @Bean
    public InitRestfulToolComponent restfulToolComponent(RestfulServicesConfig restfulServicesConfig) {
        logger.info("Starting RestfulToolComponent");
        return new InitRestfulToolComponent(restfulServicesConfig);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(InitRestfulToolComponent restfulToolComponent) {
        logger.info("Starting ToolCallbackProvider");
        return restfulToolComponent.parseRestfulInfo();
    }

}
