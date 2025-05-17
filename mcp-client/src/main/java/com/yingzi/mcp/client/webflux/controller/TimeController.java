package com.yingzi.mcp.client.webflux.controller;

import com.alibaba.cloud.ai.mcp.nacos.client.transport.LoadbalancedMcpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/10:21:52
 */
@RestController
@RequestMapping("/time")
public class TimeController {

    private final ChatClient chatClient;

//    @Autowired
//    private List<McpSyncClient> mcpSyncClients;

    @Autowired
    private List<LoadbalancedMcpSyncClient> loadbalancedMcpSyncClients;
//    @Autowired
//    private List<LoadbalancedMcpAsyncClient> loadbalancedMcpAsyncClients;

    public TimeController(ChatClient.Builder chatClientBuilder, @Qualifier("loadbalancedSyncMcpToolCallbacks") ToolCallbackProvider tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
        for (ToolCallback toolCallback : tools.getToolCallbacks()) {
            String ToolName = toolCallback.getToolDefinition().name();
            if (ToolName.equals("getCiteTimeMethod")) {
                toolCallbacks.add((ToolCallback) toolCallback);
                break;
            }
        }
        this.chatClient = chatClientBuilder
                .defaultTools(toolCallbacks)
                .build();
    }

    @RequestMapping("/chat")
    public String chatTime(@RequestParam(value = "query", defaultValue = "请告诉我现在北京时间几点了") String query) {
        return chatClient.prompt(query).call().content();
    }
    @RequestMapping("/no-weather")
    public String chatWeather(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {
        return chatClient.prompt(query).call().content();
    }

    @RequestMapping("/chat-mcp-client")
    public String chatTimeWithMcpClient(@RequestParam(value = "query", defaultValue = "请告诉我现在北京时间几点了") String query) {
        LoadbalancedMcpSyncClient loadbalancedMcpSyncClient = loadbalancedMcpSyncClients.get(0);

        return chatClient.prompt(query).call().content();
    }

}
