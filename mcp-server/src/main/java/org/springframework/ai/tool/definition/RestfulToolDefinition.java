package org.springframework.ai.tool.definition;

import org.springframework.ai.tool.support.ToolUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author yingzi
 * @date 2025/4/24:12:21
 */
public class RestfulToolDefinition implements ToolDefinition {

    private final String name;
    private final String description;
    private final String inputSchema;
    private final String serviceName;
    private final Map<String, String> methodName2Path;

    public RestfulToolDefinition(String name, String description, String inputSchema,  String serviceName, Map<String, String> methodName2Path) {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.hasText(inputSchema, "inputSchema cannot be null or empty");
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
        this.serviceName = serviceName;
        this.methodName2Path = methodName2Path;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String name() {
        return this.name;
    }

    public String description() {
        return this.description;
    }

    public String inputSchema() {
        return this.inputSchema;
    }

    public String serviceName() {
        return this.serviceName;
    }

    public Map<String, String> methodName2Path() {
        return this.methodName2Path;
    }

    @Override
    public String toString() {
        return "RestfulToolDefinition{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", inputSchema='" + inputSchema + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", methodName2Path=" + methodName2Path +
                '}';
    }

    public static class Builder {
        private String name;
        private String description;
        private String inputSchema;
        private String serviceName;
        private Map<String, String> methodName2Path;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder inputSchema(String inputSchema) {
            this.inputSchema = inputSchema;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder methodName2Path(Map<String, String> methodName2Path) {
            this.methodName2Path = methodName2Path;
            return this;
        }

        public ToolDefinition build() {
            if (!StringUtils.hasText(this.description)) {
                this.description = ToolUtils.getToolDescriptionFromName(this.name);
            }

            return new RestfulToolDefinition(this.name, this.description, this.inputSchema, this.serviceName, this.methodName2Path);
        }
    }
}
