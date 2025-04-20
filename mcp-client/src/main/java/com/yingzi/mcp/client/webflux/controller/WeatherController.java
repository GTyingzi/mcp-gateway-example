package com.yingzi.mcp.client.webflux.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
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
@RequestMapping("/weather")
public class WeatherController {

    private final ChatClient chatClient;

    public WeatherController(ChatClient.Builder chatClientBuilder, ToolCallbackProvider tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();
//        for (FunctionCallback toolCallback : tools.getToolCallbacks()) {
//            String ToolName = toolCallback.getName();
//            if (ToolName.equals("getWeatherForecastByLocation")) {
//                toolCallbacks.add((ToolCallback) toolCallback);
//                break;
//            }
//        }
        this.chatClient = chatClientBuilder
                .defaultTools(toolCallbacks)
                .build();
    }

    @RequestMapping("/no-time")
    public String chatTime(@RequestParam(value = "query", defaultValue = "请告诉我现在北京时间几点了") String query) {
        return chatClient.prompt(query).call().content();
    }

    @RequestMapping("/chat")
    public String chatWeather(@RequestParam(value = "query", defaultValue = "请告诉我北京1天以后的天气") String query) {
        return chatClient.prompt(query).call().content();
    }
}
