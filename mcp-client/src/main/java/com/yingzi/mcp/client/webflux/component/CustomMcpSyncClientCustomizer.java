package com.yingzi.mcp.client.webflux.component;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.customizer.McpSyncClientCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/22:18:07
 */
@Component
public class CustomMcpSyncClientCustomizer implements McpSyncClientCustomizer {

    private static final Logger logger = LoggerFactory.getLogger(CustomMcpSyncClientCustomizer.class);

    @Override
    public void customize(String name, McpClient.SyncSpec spec) {
        // Customize the request timeout configuration
//        spec.requestTimeout(Duration.ofSeconds(30));

        // Sets a custom sampling handler for processing message creation requests.
        spec.sampling((McpSchema.CreateMessageRequest messageRequest) -> {
            // Handle sampling
            logger.info("Sampling request: {}", messageRequest);
            return null;
        });

        // Adds a consumer to be notified when the available tools change, such as tools
        // being added or removed.
        spec.toolsChangeConsumer((List<McpSchema.Tool> tools) -> {
            // Handle tools change
            logger.info("Tools change: {}", tools);
        });

        // Adds a consumer to be notified when the available resources change, such as resources
        // being added or removed.
        spec.resourcesChangeConsumer((List<McpSchema.Resource> resources) -> {
            // Handle resources change
            logger.info("Resources change: {}", resources);
        });

        // Adds a consumer to be notified when the available prompts change, such as prompts
        // being added or removed.
        spec.promptsChangeConsumer((List<McpSchema.Prompt> prompts) -> {
            // Handle prompts change
            logger.info("Prompts change: {}", prompts);
        });

        // Adds a consumer to be notified when logging messages are received from the server.
        spec.loggingConsumer((McpSchema.LoggingMessageNotification log) -> {
            // Handle log messages
        });
    }
}
