package org.springframework.ai.tool.method;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * @author yingzi
 * @date 2025/4/7:22:17
 */
public class RestfulToolCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(RestfulToolCallback.class);

    private final ToolDefinition toolDefinition;
    // 方法名称到接口路径到映射
    private final Map<String, String> methodName2Path;
    private final WebClient webClient;
    private Map<String, String> headersMap;

    public RestfulToolCallback(ToolDefinition toolDefinition, Map<String, String> methodName2Path, WebClient webClient) {
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        Assert.notNull(methodName2Path, "methodName2Path cannot be null");
        Assert.notNull(webClient, "restClient cannot be null");
        this.toolDefinition = toolDefinition;
        this.methodName2Path = methodName2Path;
        this.webClient = webClient;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");
        logger.debug("Starting execution of tool: {}", this.toolDefinition.name());
        String path = methodName2Path.get(toolDefinition.name());
        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        StringBuilder uriBuilder = new StringBuilder().append(path).append("?");
        toolArguments.forEach((key, value) -> {
            uriBuilder.append(key).append("=").append(value).append("&");
        });
        String uri = uriBuilder.toString();
        if (uri.endsWith("&")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        String restfulResult = webClient.get().uri(uri)
                .headers(headers -> {
                    if (this.headersMap != null) {
                        this.headersMap.forEach(headers::add);
                    }
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();
        logger.debug("Successful execution of tool: {}", this.toolDefinition.name());
        assert restfulResult != null;
        return restfulResult;
    }

    public void setHeadersMap(Map<String, String> headersMap) {
        this.headersMap = headersMap;
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
        return (Map) JsonParser.fromJson(toolInput, new TypeReference<Map<String, Object>>() {
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ToolDefinition toolDefinition;
        private Map<String, String> methodName2Path;

        private WebClient webClient;

        private Builder() {
        }

        public Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }

        public Builder methodName2Path(Map<String, String> methodName2Path) {
            this.methodName2Path = methodName2Path;
            return this;
        }

        public Builder webClient(WebClient webClient) {
            this.webClient = webClient;
            return this;
        }

        public RestfulToolCallback build() {
            return new RestfulToolCallback(this.toolDefinition, this.methodName2Path, this.webClient);
        }
    }
}
