package com.yingzi.nacos.gateway.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.model.OpenApiDoc;
import com.yingzi.nacos.gateway.utils.JSONSchemaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.method.RestfulToolCallbacProvider;
import org.springframework.ai.tool.method.RestfulToolCallback;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
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
@Service
public class RestfulToolComponent {

    private static final Logger logger = LoggerFactory.getLogger(RestfulToolComponent.class);
    private final WebClient webClient;
    private Map<String, String> methodName2Path = new HashMap<>();


    public RestfulToolComponent(LoadBalancerClient loadBalancerClient) {
        String baseUrl;
        if (loadBalancerClient == null) {
            // 本地启动时，直接使用指定的端口
            baseUrl = "http://localhost:18086";
        } else {
            // 通过LoadBalancerClient动态获取服务地址
            baseUrl = loadBalancerClient.choose("mcp-restful-provider").getUri().toString();
        }

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // 解析Restful信息，注册ToolCallbackProvider
    public ToolCallbackProvider parseRestfulInfo() {
        String API_DOC_URL = "/v3/api-docs";
        String apiDocJson = this.webClient.get().uri(API_DOC_URL).retrieve().bodyToMono(String.class).block();
        ObjectMapper objectMapper = new ObjectMapper();
        List<ToolCallback> toolCallbackList = new ArrayList<>();
        try {
            OpenApiDoc openApiDoc = objectMapper.readValue(apiDocJson, OpenApiDoc.class);
            Map<String, OpenApiDoc.PathItem> paths = openApiDoc.getPaths();
            paths.forEach((path, pathItem) -> {
                if (path.equals("/echo/nacos")) {
                    return;
                }
                // 保存接口信息
                logger.info("加载path: {}", path);
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
                        .webClient(webClient)
                        .build();
                toolCallbackList.add(restfulToolCallback);
            });

        } catch (JsonProcessingException e) {
            logger.error("解析Restful Api Doc信息失败", e);
        }
        ToolCallback[] toolCallbacks = toolCallbackList.toArray(new ToolCallback[0]);
        return RestfulToolCallbacProvider.builder().toolCallbacks(toolCallbacks).build();
    }

}
