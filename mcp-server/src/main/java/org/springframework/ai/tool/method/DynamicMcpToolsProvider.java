package org.springframework.ai.tool.method;

import org.springframework.ai.tool.definition.ToolDefinition;
import java.util.List;

/**
 * @author yingzi
 * @date 2025/4/24:12:10
 */
public interface DynamicMcpToolsProvider {

    void addTool(final ToolDefinition toolDefinition);

    void removeTool(final String toolName);

}
