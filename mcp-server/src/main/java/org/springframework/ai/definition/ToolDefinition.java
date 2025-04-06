package org.springframework.ai.definition;

import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.util.ToolUtils;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.Assert;

import java.lang.reflect.Method;

/**
 * @author yingzi
 * @date 2025/4/6:17:05
 */
public interface ToolDefinition {

    String name();

    String description();

    String inputSchema();

    static DefaultToolDefinition.Builder builder() {
        return DefaultToolDefinition.builder();
    }

    static DefaultToolDefinition.Builder builder(Method method) {
        Assert.notNull(method, "method cannot be null");
        return DefaultToolDefinition.builder().name(ToolUtils.getToolName(method)).description(ToolUtils.getToolDescription(method)).inputSchema(JsonSchemaGenerator.generateForMethodInput(method, new JsonSchemaGenerator.SchemaOption[0]));
    }

    static org.springframework.ai.tool.definition.ToolDefinition from(Method method) {
        return builder(method).build();
    }
}
