package org.springframework.ai.tool.method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.util.Assert;

/**
 * @author yingzi
 * @date 2025/4/7:22:41
 */
public class RestfulToolCallbacProvider implements ToolCallbackProvider {

    private final ToolCallback[] toolCallbacks;

    private static final Logger logger = LoggerFactory.getLogger(RestfulToolCallbacProvider.class);

    public RestfulToolCallbacProvider(ToolCallback[] toolCallbacks) {
        Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
        this.toolCallbacks = toolCallbacks;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return this.toolCallbacks;
    }

    public static RestfulToolCallbacProvider.Builder builder() {
        return new RestfulToolCallbacProvider.Builder();
    }

    public static class Builder {
        private ToolCallback[] toolCallbacks;

        private Builder() {
        }

        public Builder toolCallbacks(ToolCallback... toolCallbacks) {
            this.toolCallbacks = toolCallbacks;
            return this;
        }

        public RestfulToolCallbacProvider build() {
            return new RestfulToolCallbacProvider(this.toolCallbacks);
        }
    }

}
