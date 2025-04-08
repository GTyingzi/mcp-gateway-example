package com.yingzi.nacos.gateway.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yingzi.nacos.gateway.model.OpenApiDoc;
import com.yingzi.nacos.gateway.utils.ApplicationContextHolder;
import com.yingzi.nacos.gateway.utils.JSONSchemaUtil;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.method.RestfulToolCallbacProvider;
import org.springframework.ai.tool.method.RestfulToolCallback;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
    private final RestClient restClient;
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

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // 解析Restful信息，注册ToolCallbackProvider
    public ToolCallbackProvider parseRestfulInfo() {
        String API_DOC_URL = "/v3/api-docs";
        String apiDocJson = restClient.get().uri(API_DOC_URL).retrieve().body(String.class);
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
                        .restClient(restClient)
                        .build();
                toolCallbackList.add(restfulToolCallback);
            });

        } catch (JsonProcessingException e) {
            logger.error("解析Restful Api Doc信息失败", e);
        }
        ToolCallback[] toolCallbacks = toolCallbackList.toArray(new ToolCallback[0]);
        return RestfulToolCallbacProvider.builder().toolCallbacks(toolCallbacks).build();
    }

    public String RestfulRestul(String methodName, String toolInput) {
        logger.info("调用Restful接口: {}", methodName);
        // 利用方法名去匹配选择接口
        String path = methodName2Path.get(methodName);
        // request为接口的入参
        Map<String, Object> toolArguments = extractToolArguments(toolInput);

        // 获取token
        Map<String, String> headersMap = getHeadersByWebFluxSseServerTransport();

        // 获取服务实例
        // 调用restful接口
        StringBuilder uriBuilder = new StringBuilder().append(path).append("?");
        toolArguments.forEach((key, value) -> {
            uriBuilder.append(key).append("=").append(value).append("&");
        });
        String uri = uriBuilder.toString();
        if (uri.endsWith("&")) {
            uri = uri.substring(0, uri.length() - 1);
        }

        String restfulResult = restClient.get().uri(uri)
                .headers(
                        headers -> {
                            if (headersMap != null) {
                                headersMap.forEach(headers::add);
                            }
                        }
                )
                .retrieve()
                .body(String.class);

        return restfulResult;
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
        return (Map) JsonParser.fromJson(toolInput, new TypeReference<Map<String, Object>>() {
        });
    }

    private Map<String, String> getHeadersByWebFluxSseServerTransport() {
        // 获取WebFluxSseServerTransport组件，从中获取请求头信息
        WebFluxSseServerTransport webFluxSseServerTransport = ApplicationContextHolder.getBean(WebFluxSseServerTransport.class);
        return webFluxSseServerTransport.getHeadersMap();
    }

    public static void main(String[] args) {
        // 在本地启动时，传入null以使用指定端口
        RestfulToolComponent restfulToolComponent = new RestfulToolComponent(null);
        restfulToolComponent.parseRestfulInfo();

        String methodName = "getWeatherForecastByLocation";
        String toolInput = "{\"latitude\":39.9042,\"longitude\":116.4074}";
        String result = restfulToolComponent.RestfulRestul(methodName, toolInput);
        System.out.println(result);
    }

}
