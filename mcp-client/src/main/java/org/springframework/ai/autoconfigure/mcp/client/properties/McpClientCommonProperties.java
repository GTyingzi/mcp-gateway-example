package org.springframework.ai.autoconfigure.mcp.client.properties;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yingzi
 * @date 2025/4/8:10:50
 */
@ConfigurationProperties("spring.ai.mcp.client")
public class McpClientCommonProperties {
    public static final String CONFIG_PREFIX = "spring.ai.mcp.client";
    private boolean enabled = true;
    private String name = "spring-ai-mcp-client";
    private String version = "1.0.0";
    private boolean initialized = true;
    private Duration requestTimeout = Duration.ofSeconds(20L);
    private ClientType type;
    private boolean rootChangeNotification;

    public McpClientCommonProperties() {
        this.type = McpClientCommonProperties.ClientType.SYNC;
        this.rootChangeNotification = true;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public Duration getRequestTimeout() {
        return this.requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public ClientType getType() {
        return this.type;
    }

    public void setType(ClientType type) {
        this.type = type;
    }

    public boolean isRootChangeNotification() {
        return this.rootChangeNotification;
    }

    public void setRootChangeNotification(boolean rootChangeNotification) {
        this.rootChangeNotification = rootChangeNotification;
    }


    public static enum ClientType {
        SYNC,
        ASYNC;

        private ClientType() {
        }
    }
}