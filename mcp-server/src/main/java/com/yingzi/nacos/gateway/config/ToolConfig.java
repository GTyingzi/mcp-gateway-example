package com.yingzi.nacos.gateway.config;

import com.yingzi.nacos.gateway.component.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;

/**
 * @author yingzi
 * @date 2025/4/6:13:34
 */
@Configuration
public class ToolConfig {

    private static final Logger logger = LoggerFactory.getLogger(ToolConfig.class);

//    @Bean
//    public ToolCallbackProvider weatherTools(TimeService timeService) {
//        logger.info("注入工具信息: {}", timeService);
//        return MethodToolCallbackProvider.builder().toolObjects(timeService).build();
//    }

}