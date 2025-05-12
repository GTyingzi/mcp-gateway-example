package com.yingzi.nacos.gateway.component;

import com.alibaba.nacos.api.config.ConfigChangeEvent;
import com.alibaba.nacos.api.config.ConfigChangeItem;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.config.listener.impl.AbstractConfigChangeListener;
import com.alibaba.nacos.common.utils.JacksonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.model.OpenApiDoc;
import com.yingzi.nacos.gateway.utils.ApplicationContextHolder;
import com.yingzi.nacos.gateway.utils.JSONSchemaUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.definition.RestfulToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.DynamicMcpToolsProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

/**
 * @author yingzi
 * @date 2025/4/24:12:48
 */
public class DynamicRestfulToolsWatch extends AbstractConfigChangeListener implements EventListener {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRestfulToolsWatch.class);
    private final NamingService namingService;
    private final DynamicMcpToolsProvider dynamicMcpToolsProvider;
    private String changeServiceName = null;
    private final Map<String, Set<String>> service2tool;
    private static final String API_DOC_URL = "/v3/api-docs";

    public DynamicRestfulToolsWatch(NamingService namingService, DynamicMcpToolsProvider dynamicMcpToolsProvider) {
        this.namingService = namingService;
        this.dynamicMcpToolsProvider = dynamicMcpToolsProvider;
        InitRestfulToolComponent initRestfulToolComponent = ApplicationContextHolder.getBean(InitRestfulToolComponent.class);
        this.service2tool = initRestfulToolComponent.getService2tool();
        initLinster();
    }

    private void initLinster() {
        for (String serviceName : service2tool.keySet()) {
            try {
                namingService.subscribe(serviceName, this);
            } catch (NacosException e) {
                logger.info("Failed to get tool config for service: {}", serviceName, e);
            }
        }
    }

    private void parse(String serviceName) {
        WebClient globalWebClient = ApplicationContextHolder.getBean(WebClient.class);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, true);
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
            Set<String> toolNames = new HashSet<>();
            paths.forEach((path, pathItem) -> {
                // 保存接口信息，方法名可能重复，后续考虑追加服务名保证唯一性
                String methodName = pathItem.operation().getMethodName();

                methodName2Path.put(methodName, path);
                // 构建RestfulToolDefinition对象
                ToolDefinition toolDefinition = RestfulToolDefinition.builder()
                        .name(methodName)
                        .description(pathItem.getGetOperation().getDescription())
                        .inputSchema(JSONSchemaUtil.getInputSchema(pathItem.operation().getParameters()))
                        .serviceName(serviceName)
                        .methodName2Path(methodName2Path)
                        .build();

                logger.info("Loaded tool definition: {}", toolDefinition);
                dynamicMcpToolsProvider.addTool(toolDefinition);

                toolNames.add(methodName);
            });
            service2tool.put(serviceName, toolNames);

        } catch (JsonProcessingException e) {
            logger.error("解析Restful Api Doc信息失败，服务名称: {}", changeServiceName, e);
        } catch (Exception e) {
            logger.error("获取Restful Api Doc信息失败，服务名称: {}", changeServiceName, e);
        }
    }

    private void removeToolByService(String serviceName) {
        if (service2tool.containsKey(serviceName)) {
            for (String toolName : service2tool.get(serviceName)) {
                dynamicMcpToolsProvider.removeTool(toolName);
                service2tool.remove(serviceName);
                logger.info("Removed tool: {}", toolName);
            }
        }
    }

    @Override
    public void onEvent(Event event) {
        logger.info("Received event: {}", event);
        if (event instanceof NamingEvent namingEvent) {
            // 获取服务名称
            logger.info("Received service instance change event for service: {}", namingEvent.getServiceName());
            this.changeServiceName = namingEvent.getServiceName();
            List<Instance> instances = namingEvent.getInstances();
            logger.info("Updated instances count: {}", instances.size());
            // 打印每个实例的详细信息
            instances.forEach(instance -> {
                logger.info("Instance: {}:{} (Healthy: {}, Enabled: {}, Metadata: {})", instance.getIp(),
                        instance.getPort(), instance.isHealthy(), instance.isEnabled(),
                        JacksonUtils.toJson(instance.getMetadata()));
            });
            removeToolByService(changeServiceName);

            // 检查服务是否仍然可用
            if (!instances.isEmpty()) {
                parse(changeServiceName);
            }
        }
    }

    @Override
    public void receiveConfigChange(ConfigChangeEvent event) {
        for (ConfigChangeItem item : event.getChangeItems()) {
            String dataId = item.getKey();
            if (dataId.startsWith("mcp-server-provider")) {
                logger.info("Received config change event for dataId: {}", dataId);
            }
        }
    }
}
