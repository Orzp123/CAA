package com.caa.config;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.default-provider:openai}")
    private String defaultProvider;

    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatModel openAiChatModel,
                                  AnthropicChatModel anthropicChatModel) {
        if ("anthropic".equalsIgnoreCase(defaultProvider)) {
            return ChatClient.builder(anthropicChatModel).build();
        }
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean("openAiChatClient")
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    @Bean("anthropicChatClient")
    public ChatClient anthropicChatClient(AnthropicChatModel anthropicChatModel) {
        return ChatClient.builder(anthropicChatModel).build();
    }
}
