package org.springframework.ai.mcp.server.autoconfigure;

import com.alibaba.cloud.ai.mcp.nacos.NacosMcpRegistryProperties;
import com.alibaba.cloud.ai.mcp.nacos.common.NacosMcpProperties;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
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

import java.util.Properties;

/**
 * @author yingzi
 * @date 2025/4/24:13:35
 */
@AutoConfiguration
@EnableConfigurationProperties({RestfulServicesConfig.class, NacosMcpProperties.class, NacosMcpRegistryProperties.class})
public class NacosMcpRestfulAutoConfiguration {

    private static final LogAccessor logger = new LogAccessor(McpServerAutoConfiguration.class);

    public NacosMcpRestfulAutoConfiguration() {
    }

    @Bean
    public NamingService namingService(NacosMcpProperties nacosMcpProperties, NacosMcpRegistryProperties nacosMcpRegistryProperties) {
        Properties nacosProperties = nacosMcpProperties.getNacosProperties();
        nacosProperties.put(PropertyKeyConst.NAMESPACE, nacosMcpRegistryProperties.getServiceNamespace());

        try {
            return NamingFactory.createNamingService(nacosProperties);
        }
        catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public DynamicRestfulToolsWatch dynamicRestfulToolsWatch(NamingService namingService, DynamicMcpSyncToolsProvider dynamicMcpSyncToolsProvider) {
        logger.info("Starting DynamicRestfulToolsWatch");
        return new DynamicRestfulToolsWatch(namingService, dynamicMcpSyncToolsProvider);
    }

    @Bean
    public DynamicMcpSyncToolsProvider dynamicMcpSyncToolsProvider(McpSyncServer mcpSyncServer) {
        logger.info("Starting DynamicMcpSyncToolsProvider");
        return new DynamicMcpSyncToolsProvider(mcpSyncServer);
    }

    @Bean
    public InitRestfulToolComponent restfulToolComponent(RestfulServicesConfig restfulServicesConfig, NamingService namingService) {
        logger.info("Starting RestfulToolComponent");
        return new InitRestfulToolComponent(restfulServicesConfig, namingService);
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(InitRestfulToolComponent restfulToolComponent) {
        logger.info("Starting ToolCallbackProvider");
        return restfulToolComponent.parseRestfulInfo();
    }

}
