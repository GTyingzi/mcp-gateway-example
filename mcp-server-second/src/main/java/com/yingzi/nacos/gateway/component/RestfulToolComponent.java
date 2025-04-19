package com.yingzi.nacos.gateway.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.config.RestfulServicesConfig;
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
    private final List<WebClient> webClientList;
    private final Map<String, WebClient> webClientMap;

    public RestfulToolComponent(LoadBalancerClient loadBalancerClient, RestfulServicesConfig restfulServicesConfig) {
        webClientList = new ArrayList<>();
        webClientMap = new HashMap<>();
        for (String restfulService : restfulServicesConfig.getRestfulServices()) {
            String baseUrl = loadBalancerClient.choose(restfulService).getUri().toString();
            logger.info("LoadBalancerClient choose: {}, restfulService: {}", baseUrl, restfulService);
            WebClient webClient = WebClient.builder()
                    .baseUrl(baseUrl)
                    .build();
            webClientList.add(webClient);
            webClientMap.put(restfulService, webClient);
        }
    }

    // 解析Restful信息，注册ToolCallbackProvider
    public ToolCallbackProvider parseRestfulInfo() {
        ObjectMapper objectMapper = new ObjectMapper();
        List<ToolCallback> toolCallbackList = new ArrayList<>();

        String API_DOC_URL = "/v3/api-docs";
        webClientList.forEach(webClient -> {
            String serviceName = webClientMap.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(webClient))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("Unknown Service"); // 获取当前 WebClient 对应的服务名称

            try {
                String apiDocJson = webClient.get().uri(API_DOC_URL).retrieve().bodyToMono(String.class).block();
                OpenApiDoc openApiDoc = objectMapper.readValue(apiDocJson, OpenApiDoc.class);
                Map<String, OpenApiDoc.PathItem> paths = openApiDoc.getPaths();
                Map<String, String> methodName2Path = new HashMap<>();
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
                logger.error("解析Restful Api Doc信息失败，服务名称: {}", serviceName, e); // 记录失败的服务名称
            } catch (Exception e) {
                logger.error("获取Restful Api Doc信息失败，服务名称: {}", serviceName, e); // 记录失败的服务名称
            }
        });

        ToolCallback[] toolCallbacks = toolCallbackList.toArray(new ToolCallback[0]);
        return RestfulToolCallbacProvider.builder().toolCallbacks(toolCallbacks).build();
    }


}
