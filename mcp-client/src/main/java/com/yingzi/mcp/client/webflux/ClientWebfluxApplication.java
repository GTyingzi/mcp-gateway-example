package com.yingzi.mcp.client.webflux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.autoconfigure.SseHttpClientTransportAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestClient;

import java.util.Scanner;

@SpringBootApplication(exclude = {
        SseHttpClientTransportAutoConfiguration.class,
})
@ComponentScan(basePackages = {"com.alibaba.cloud.ai.mcp.nacos", "com.yingzi.mcp.client.webflux"})
public class ClientWebfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientWebfluxApplication.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, @Qualifier("loadbalancedMcpAsyncToolCallbacks") ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            var chatClient = chatClientBuilder
                    .defaultTools(tools)
                    .build();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\n>>> QUESTION: ");
                String userInput = scanner.nextLine();
                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
                if (userInput.isEmpty()) {
                    userInput = "北京时间现在几点钟";
                }
                System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
            }
            scanner.close();
            context.close();
        };
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}