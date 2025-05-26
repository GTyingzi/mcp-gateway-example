package org.springframework.ai.tool.method;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yingzi.nacos.gateway.utils.ApplicationContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.RestfulToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/4/7:22:17
 */
public class RestfulToolCallback implements ToolCallback {

    private static final Logger logger = LoggerFactory.getLogger(RestfulToolCallback.class);
    private final RestfulToolDefinition toolDefinition;
    private final WebClient webClient;
    private Map<String, String> headersMap;


    public RestfulToolCallback(ToolDefinition toolDefinition) {
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        Assert.isInstanceOf(RestfulToolDefinition.class, toolDefinition, "toolDefinition must be an instance of RestfulToolDefinition");
        this.toolDefinition = (RestfulToolDefinition) toolDefinition;
        this.webClient = ApplicationContextUtil.getBean(WebClient.class);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        return this.call(toolInput, (ToolContext) null);
    }

    public String call(String toolInput, @Nullable ToolContext toolContext) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");
        logger.debug("Starting execution of tool: {}", this.toolDefinition.name());
        
        NamingService namingService = ApplicationContextUtil.getBean(NamingService.class);
        
        String path = toolDefinition.methodName2Path().get(toolDefinition.name());
        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        StringBuilder uriBuilder = new StringBuilder().append(path).append("?");
        toolArguments.forEach((key, value) -> {
            uriBuilder.append(key).append("=").append(value).append("&");
        });
        String uri = uriBuilder.toString();
        if (uri.endsWith("&")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        List<Instance> instances = null;
        try {
            instances = namingService.selectInstances(toolDefinition.serviceName(), true);
        } catch (NacosException e) {
            logger.error("解析Restful 信息失败，服务名称: {}", toolDefinition.serviceName(), e);
        }
        if (instances.isEmpty()) {
            logger.error("No available service instance for {}", toolDefinition.serviceName());
        }
        Instance instance = instances.get(0);
        String url = instance.getMetadata().getOrDefault("scheme", "http") + "://" + instance.getIp() + ":"
                + instance.getPort();

        url = url + uri;
        logger.info("Calling restful service: {}", url);
        String restfulResult = webClient.get()
                .uri(url)
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
        return new RestfulToolCallback.Builder();
    }

    public static class Builder {
        private ToolDefinition toolDefinition;

        private Builder() {
        }

        public Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }

        public RestfulToolCallback build() {
            return new RestfulToolCallback(this.toolDefinition);
        }
    }
}
