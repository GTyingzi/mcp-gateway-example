package com.alibaba.cloud.ai.mcp.nacos.client;

import org.springframework.ai.mcp.client.autoconfigure.properties.McpSseClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

/**
 *
 * @author yingzi
 * @date 2025/4/29:17:01
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.mcp.client.sse")
public class NacosMcpSseClientProperties {
    private Map<String, String> connections;

    public NacosMcpSseClientProperties() {
    }

    public Map<String, String> getConnections() {
        return connections;
    }

    public void setConnections(Map<String, String> connections) {
        this.connections = connections;
    }

//    public static record NacosParameters(String serviceName, String group, String dataId) {
//        public NacosParameters(String serviceName, String group,  String dataId) {
//            this.serviceName = serviceName;
//            this.group = group;
//            this.dataId = dataId;
//        }
//    }
}