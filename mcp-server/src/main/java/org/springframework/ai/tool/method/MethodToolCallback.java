package org.springframework.ai.tool.method;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author yingzi
 * @date 2025/4/6:18:13
 */
public class MethodToolCallback implements ToolCallback {
    private static final Logger logger = LoggerFactory.getLogger(MethodToolCallback.class);
    private static final ToolCallResultConverter DEFAULT_RESULT_CONVERTER = new DefaultToolCallResultConverter();
    private static final ToolMetadata DEFAULT_TOOL_METADATA = ToolMetadata.builder().build();
    private final ToolDefinition toolDefinition;
    private final ToolMetadata toolMetadata;
    private final Method toolMethod;
    @Nullable
    private final Object toolObject;
    private final ToolCallResultConverter toolCallResultConverter;

    public MethodToolCallback(ToolDefinition toolDefinition, @Nullable ToolMetadata toolMetadata, Method toolMethod, @Nullable Object toolObject, @Nullable ToolCallResultConverter toolCallResultConverter) {
        Assert.notNull(toolDefinition, "toolDefinition cannot be null");
        this.toolDefinition = toolDefinition;
        this.toolMetadata = toolMetadata != null ? toolMetadata : DEFAULT_TOOL_METADATA;
        this.toolMethod = toolMethod;
        this.toolObject = toolObject;
        this.toolCallResultConverter = toolCallResultConverter != null ? toolCallResultConverter : DEFAULT_RESULT_CONVERTER;
    }

    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    public ToolMetadata getToolMetadata() {
        return this.toolMetadata;
    }

    public String call(String toolInput) {
        return this.call(toolInput, (ToolContext)null);
    }

    public String call(String toolInput, @Nullable ToolContext toolContext) {
        Assert.hasText(toolInput, "toolInput cannot be null or empty");
        logger.debug("Starting execution of tool: {}", this.toolDefinition.name());
        this.validateToolContextSupport(toolContext);
        Map<String, Object> toolArguments = this.extractToolArguments(toolInput);
        Object[] methodArguments = this.buildMethodArguments(toolArguments, toolContext);
        Object result = this.callMethod(methodArguments);
        logger.debug("Successful execution of tool: {}", this.toolDefinition.name());
        Type returnType = this.toolMethod.getGenericReturnType();
        return this.toolCallResultConverter.convert(result, returnType);
    }

    private void validateToolContextSupport(@Nullable ToolContext toolContext) {
        boolean isToolContextRequired = toolContext != null && !CollectionUtils.isEmpty(toolContext.getContext());
        boolean isToolContextAcceptedByMethod = Stream.of(this.toolMethod.getParameterTypes()).anyMatch((type) -> {
            return ClassUtils.isAssignable(type, ToolContext.class);
        });
        if (isToolContextRequired && !isToolContextAcceptedByMethod) {
            throw new IllegalArgumentException("ToolContext is not supported by the method as an argument");
        }
    }

    private Map<String, Object> extractToolArguments(String toolInput) {
        return (Map) JsonParser.fromJson(toolInput, new TypeReference<Map<String, Object>>() {
        });
    }

    private Object[] buildMethodArguments(Map<String, Object> toolInputArguments, @Nullable ToolContext toolContext) {
        return Stream.of(this.toolMethod.getParameters()).map((parameter) -> {
            if (parameter.getType().isAssignableFrom(ToolContext.class)) {
                return toolContext;
            } else {
                Object rawArgument = toolInputArguments.get(parameter.getName());
                return this.buildTypedArgument(rawArgument, parameter.getType());
            }
        }).toArray();
    }

    @Nullable
    private Object buildTypedArgument(@Nullable Object value, Class<?> type) {
        return value == null ? null : JsonParser.toTypedObject(value, type);
    }

    @Nullable
    private Object callMethod(Object[] methodArguments) {
        if (this.isObjectNotPublic() || this.isMethodNotPublic()) {
            this.toolMethod.setAccessible(true);
        }

        try {
            Object result = this.toolMethod.invoke(this.toolObject, methodArguments);
            return result;
        } catch (IllegalAccessException var4) {
            throw new IllegalStateException("Could not access method: " + var4.getMessage(), var4);
        } catch (InvocationTargetException var5) {
            throw new ToolExecutionException(this.toolDefinition, var5.getCause());
        }
    }

    private boolean isObjectNotPublic() {
        return this.toolObject != null && !Modifier.isPublic(this.toolObject.getClass().getModifiers());
    }

    private boolean isMethodNotPublic() {
        return !Modifier.isPublic(this.toolMethod.getModifiers());
    }

    public String toString() {
        String var10000 = String.valueOf(this.toolDefinition);
        return "MethodToolCallback{toolDefinition=" + var10000 + ", toolMetadata=" + String.valueOf(this.toolMetadata) + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ToolDefinition toolDefinition;
        private ToolMetadata toolMetadata;
        private Method toolMethod;
        private Object toolObject;
        private ToolCallResultConverter toolCallResultConverter;

        private Builder() {
        }

        public Builder toolDefinition(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
            return this;
        }

        public Builder toolMetadata(ToolMetadata toolMetadata) {
            this.toolMetadata = toolMetadata;
            return this;
        }

        public Builder toolMethod(Method toolMethod) {
            this.toolMethod = toolMethod;
            return this;
        }

        public Builder toolObject(Object toolObject) {
            this.toolObject = toolObject;
            return this;
        }

        public Builder toolCallResultConverter(ToolCallResultConverter toolCallResultConverter) {
            this.toolCallResultConverter = toolCallResultConverter;
            return this;
        }

        public MethodToolCallback build() {
            return new MethodToolCallback(this.toolDefinition, this.toolMetadata, this.toolMethod, this.toolObject, this.toolCallResultConverter);
        }
    }
}

