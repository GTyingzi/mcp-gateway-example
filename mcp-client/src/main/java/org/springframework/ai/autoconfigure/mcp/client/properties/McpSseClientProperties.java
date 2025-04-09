package org.springframework.ai.autoconfigure.mcp.client.properties;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.MultiValueMap;

/**
 * @author yingzi
 * @date 2025/4/8:13:21
 */
@ConfigurationProperties("spring.ai.mcp.client.sse")
public class McpSseClientProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.client.sse";
    private final Map<String, SseParameters> connections = new HashMap();

    public McpSseClientProperties() {
    }

    public Map<String, SseParameters> getConnections() {
        return this.connections;
    }

    public static record SseParameters(String url, Map<String, String> headersMap) {

        public String url() {
            return this.url;
        }
        public Map<String, String> headersMap() {
            return this.headersMap;
        }
    }
}
