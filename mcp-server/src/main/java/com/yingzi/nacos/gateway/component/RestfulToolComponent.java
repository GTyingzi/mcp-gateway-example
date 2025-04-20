package com.yingzi.nacos.gateway.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.config.RestfulServicesConfig;
import com.yingzi.nacos.gateway.model.OpenApiDoc;
import com.yingzi.nacos.gateway.utils.ApplicationContextHolder;
import com.yingzi.nacos.gateway.utils.JSONSchemaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.method.RestfulToolCallbacProvider;
import org.springframework.ai.tool.method.RestfulToolCallback;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/4/6:13:31
 * 解析Restful信息，注册ToolCallbackProvider
 */
@Component
public class RestfulToolComponent {

    private static final Logger logger = LoggerFactory.getLogger(RestfulToolComponent.class);
    private static final String API_DOC_URL = "/v3/api-docs";

    private final LoadBalancerClient loadBalancerClient;
    private final ObjectMapper objectMapper;
    private final List<ToolCallback> toolCallbackList;
    private final RestfulServicesConfig restfulServicesConfig;

    public RestfulToolComponent(LoadBalancerClient loadBalancerClient, RestfulServicesConfig restfulServicesConfig) {
        this.loadBalancerClient = loadBalancerClient;
        this.objectMapper = new ObjectMapper();
        this.toolCallbackList = new ArrayList<>();
        this.restfulServicesConfig = restfulServicesConfig;
    }

    private void initializeTools() {
        for (String serviceName : restfulServicesConfig.getRestfulServices()) {
            try {
                // 使用 LoadBalancerClient 获取服务实例
                ServiceInstance instance = loadBalancerClient.choose(serviceName);
                if (instance == null) {
                    logger.error("No available service instance for {}", serviceName);
                    continue;
                }

                String url = instance.getUri().toString() + API_DOC_URL;
                WebClient globalWebClient = ApplicationContextHolder.getBean(WebClient.class);
                String apiDocJson = globalWebClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                OpenApiDoc openApiDoc = objectMapper.readValue(apiDocJson, OpenApiDoc.class);
                Map<String, OpenApiDoc.PathItem> paths = openApiDoc.getPaths();
                Map<String, String> methodName2Path = new HashMap<>();
                
                paths.forEach((path, pathItem) -> {
                    if (path.equals("/echo/nacos")) {
                        return;
                    }
                    // 保存接口信息
                    logger.info("Loading path for service {}: {}", serviceName, path);
                    String methodName = pathItem.operation().getMethodName();
                    methodName2Path.put(methodName, path);
                    // 构建toolObject对象
                    RestfulToolCallback restfulToolCallback = RestfulToolCallback.builder()
                            .toolDefinition(DefaultToolDefinition.builder()
                                    .name(methodName)
                                    .description(pathItem.getGetOperation().getDescription())
                                    .inputSchema(JSONSchemaUtil.getInputSchema(pathItem.operation().getParameters()))
                                    .build()
                            )
                            .methodName2Path(methodName2Path)
                            .serviceId(serviceName)
                            .build();
                    toolCallbackList.add(restfulToolCallback);
                });
            } catch (JsonProcessingException e) {
                logger.error("解析Restful Api Doc信息失败，服务名称: {}", serviceName, e);
            } catch (Exception e) {
                logger.error("获取Restful Api Doc信息失败，服务名称: {}", serviceName, e);
            }
        }
    }

    public List<ToolCallback> getToolCallbackList() {
        return toolCallbackList;
    }

    public ToolCallbackProvider parseRestfulInfo() {
        initializeTools();
        ToolCallback[] toolCallbacks = toolCallbackList.toArray(new ToolCallback[0]);
        return RestfulToolCallbacProvider.builder().toolCallbacks(toolCallbacks).build();
    }
}
