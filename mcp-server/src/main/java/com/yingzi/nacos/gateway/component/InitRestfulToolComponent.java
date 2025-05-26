package com.yingzi.nacos.gateway.component;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.config.RestfulServicesConfig;
import com.yingzi.nacos.gateway.model.OpenApiDoc;
import com.yingzi.nacos.gateway.utils.ApplicationContextUtil;
import com.yingzi.nacos.gateway.utils.JSONSchemaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.RestfulToolDefinition;
import org.springframework.ai.tool.method.RestfulToolCallbacProvider;
import org.springframework.ai.tool.method.RestfulToolCallback;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * @author yingzi
 * @date 2025/4/6:13:31
 * 解析Restful信息，注册ToolCallbackProvider
 */
public class InitRestfulToolComponent implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(InitRestfulToolComponent.class);
    private static final String API_DOC_URL = "/v3/api-docs";
    private final ObjectMapper objectMapper;
    private final List<ToolCallback> toolCallbackList;
    private final RestfulServicesConfig restfulServicesConfig;
    private final NamingService namingService;

    public InitRestfulToolComponent(RestfulServicesConfig restfulServicesConfig, NamingService namingService) {
        this.namingService = namingService;
        this.objectMapper = new ObjectMapper();
        this.toolCallbackList = new ArrayList<>();
        this.restfulServicesConfig = restfulServicesConfig;
    }

    private void initializeTools() {
        WebClient globalWebClient = ApplicationContextUtil.getBean(WebClient.class);

        for (String serviceName : restfulServicesConfig.getRestfulServices()) {
            try {
                List<Instance> instances = namingService.selectInstances(serviceName, "DEFAULT_GROUP",true);
                if (instances.isEmpty()) {
                    logger.error("No available service instance for {}", serviceName);
                }
                Instance instance = instances.get(0);
                String url = instance.getMetadata().getOrDefault("scheme", "http") + "://" + instance.getIp() + ":"
                        + instance.getPort();
                url = url + API_DOC_URL;

                String apiDocJson = globalWebClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                OpenApiDoc openApiDoc = objectMapper.readValue(apiDocJson, OpenApiDoc.class);
                Map<String, OpenApiDoc.PathItem> paths = openApiDoc.getPaths();
                Map<String, String> methodName2Path = new HashMap<>();
                
                paths.forEach((path, pathItem) -> {
                    // 保存接口信息
                    logger.info("Loading path for service {}: {}", serviceName, path);
                    String methodName = pathItem.operation().getMethodName();
                    methodName2Path.put(methodName, path);
                    // 构建toolObject对象
                    RestfulToolCallback restfulToolCallback = RestfulToolCallback.builder()
                            .toolDefinition(RestfulToolDefinition.builder()
                                    .name(methodName)
                                    .description(pathItem.getGetOperation().getDescription())
                                    .inputSchema(JSONSchemaUtil.getInputSchema(pathItem.operation().getParameters()))
                                    .serviceName(serviceName)
                                    .methodName2Path(methodName2Path)
                                    .build()
                            )
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

    public Map<String, Set<String>> getService2tool() {
        Map<String, Set<String>> service2tool = new HashMap<>();
        for (ToolCallback toolCallback : toolCallbackList) {
            RestfulToolCallback restfulToolCallback = (RestfulToolCallback) toolCallback;
            RestfulToolDefinition restfulToolDefinition = (RestfulToolDefinition) restfulToolCallback.getToolDefinition();

            String serviceName = restfulToolDefinition.serviceName();
            String toolName = restfulToolDefinition.name();
            if (!service2tool.containsKey(serviceName)) {
                service2tool.put(serviceName, new HashSet<>());
            }
            service2tool.get(serviceName).add(toolName);
        }
        return service2tool;
    }

    public ToolCallbackProvider parseRestfulInfo() {
        initializeTools();
        ToolCallback[] toolCallbacks = toolCallbackList.toArray(new ToolCallback[0]);
        return RestfulToolCallbacProvider.builder().toolCallbacks(toolCallbacks).build();
    }

}
