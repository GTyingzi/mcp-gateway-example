package org.springframework.ai.mcp;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/6:15:32
 */
public final class McpToolUtils {

    private McpToolUtils() {
    }

    public static List<McpServerFeatures.SyncToolRegistration> toSyncToolRegistration(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream().map(McpToolUtils::toSyncToolRegistration).toList();
    }

    public static List<McpServerFeatures.SyncToolRegistration> toSyncToolRegistration(ToolCallback... toolCallbacks) {
        return toSyncToolRegistration(List.of(toolCallbacks));
    }

    // 重写该方法
    public static McpServerFeatures.SyncToolRegistration toSyncToolRegistration(ToolCallback toolCallback) {
        McpSchema.Tool tool = new McpSchema.Tool(toolCallback.getToolDefinition().name(), toolCallback.getToolDefinition().description(), toolCallback.getToolDefinition().inputSchema());
        return new McpServerFeatures.SyncToolRegistration(tool, (request) -> {
            try {
                String callResult = toolCallback.call(ModelOptionsUtils.toJsonString(request));
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(callResult)), false);
            } catch (Exception var3) {
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(var3.getMessage())), true);
            }
        });
    }

    public static List<McpServerFeatures.AsyncToolRegistration> toAsyncToolRegistration(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream().map(McpToolUtils::toAsyncToolRegistration).toList();
    }

    public static List<McpServerFeatures.AsyncToolRegistration> toAsyncToolRegistration(ToolCallback... toolCallbacks) {
        return toAsyncToolRegistration(List.of(toolCallbacks));
    }

    public static McpServerFeatures.AsyncToolRegistration toAsyncToolRegistration(ToolCallback toolCallback) {
        McpServerFeatures.SyncToolRegistration syncToolRegistration = toSyncToolRegistration(toolCallback);
        return new McpServerFeatures.AsyncToolRegistration(syncToolRegistration.tool(), (map) -> {
            return Mono.fromCallable(() -> {
                return (McpSchema.CallToolResult)syncToolRegistration.call().apply(map);
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    public static List<ToolCallback> getToolCallbacksFromSyncClients(McpSyncClient... mcpClients) {
        return getToolCallbacksFromSyncClients(List.of(mcpClients));
    }

    public static List<ToolCallback> getToolCallbacksFromSyncClients(List<McpSyncClient> mcpClients) {
        return CollectionUtils.isEmpty(mcpClients) ? List.of() : List.of((new SyncMcpToolCallbackProvider(mcpClients)).getToolCallbacks());
    }

    public static List<ToolCallback> getToolCallbacksFromAsyncClients(McpAsyncClient... asynMcpClients) {
        return getToolCallbacksFromAsyncClinents(List.of(asynMcpClients));
    }

    public static List<ToolCallback> getToolCallbacksFromAsyncClinents(List<McpAsyncClient> asynMcpClients) {
        return CollectionUtils.isEmpty(asynMcpClients) ? List.of() : List.of((new AsyncMcpToolCallbackProvider(asynMcpClients)).getToolCallbacks());
    }
}
