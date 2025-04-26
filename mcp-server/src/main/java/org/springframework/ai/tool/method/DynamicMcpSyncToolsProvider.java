package org.springframework.ai.tool.method;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.mcp.McpToolUtils;

import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/24:12:11
 */
public class DynamicMcpSyncToolsProvider implements DynamicMcpToolsProvider {
    private final McpSyncServer mcpSyncServer;

    public DynamicMcpSyncToolsProvider(final McpSyncServer mcpSyncServer) {
        this.mcpSyncServer = mcpSyncServer;
    }

    @Override
    public void addTool(final ToolDefinition toolDefinition) {
        RestfulToolCallback restfulToolCallback = new RestfulToolCallback(toolDefinition);
        mcpSyncServer.addTool(McpToolUtils.toSyncToolSpecification(restfulToolCallback));
    }

    @Override
    public void removeTool(final String toolName) {
        mcpSyncServer.removeTool(toolName);
    }

}
