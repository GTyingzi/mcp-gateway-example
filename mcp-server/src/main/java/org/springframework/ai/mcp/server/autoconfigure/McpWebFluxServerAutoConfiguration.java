//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.springframework.ai.mcp.server.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.server.RouterFunction;

@AutoConfiguration
@ConditionalOnClass({WebFluxSseServerTransportProvider.class})
@ConditionalOnMissingBean({McpServerTransportProvider.class})
@ConditionalOnProperty(
        prefix = "spring.ai.mcp.server",
        name = {"stdio"},
        havingValue = "false",
        matchIfMissing = true
)
public class McpWebFluxServerAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpWebFluxServerAutoConfiguration.class);

    public McpWebFluxServerAutoConfiguration() {
    }

    @Autowired
    private Environment environment;

    private int getServerPort() {
        // 获取服务器端口，默认值为8080（如果未设置）
        String port = environment.getProperty("server.port", "8080");
        return Integer.parseInt(port);
    }

    @Bean
    @ConditionalOnMissingBean
    public WebFluxSseServerTransportProvider webFluxTransport(ObjectProvider<ObjectMapper> objectMapperProvider, McpServerProperties serverProperties) {
        ObjectMapper objectMapper = (ObjectMapper)objectMapperProvider.getIfAvailable(ObjectMapper::new);
        // 获取当前服务器的端口号
        String baseUrl = "http://10.38.149.106" + ":" + getServerPort();
        logger.info("baseUrl: {}", baseUrl);

        return new WebFluxSseServerTransportProvider(baseUrl, objectMapper, serverProperties.getSseMessageEndpoint());
    }

    @Bean
    public RouterFunction<?> webfluxMcpRouterFunction(WebFluxSseServerTransportProvider webFluxProvider) {
        return webFluxProvider.getRouterFunction();
    }
}
