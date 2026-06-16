package com.caa.chat.service;

import com.caa.agent.model.AgentEntity;
import com.caa.agent.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final AgentService agentService;
    private final ChatClient openAiChatClient;
    private final ChatClient anthropicChatClient;

    public ChatService(AgentService agentService,
                       @Qualifier("openAiChatClient") ChatClient openAiChatClient,
                       @Qualifier("anthropicChatClient") ChatClient anthropicChatClient) {
        this.agentService = agentService;
        this.openAiChatClient = openAiChatClient;
        this.anthropicChatClient = anthropicChatClient;
    }

    public String chat(String agentId, String userMessage, List<Map<String, String>> messages) {
        AgentEntity agent = agentService.findEntityById(agentId);
        ChatClient client = selectClient(agent.getProvider());

        List<Message> history = buildMessages(messages);
        // If a direct userMessage string was provided (legacy), append it
        if (userMessage != null && !userMessage.isBlank()) {
            history.add(new UserMessage(userMessage));
        }

        return client.prompt()
                .system(systemPrompt(agent))
                .messages(history)
                .call()
                .content();
    }

    public Flux<String> chatStream(String agentId, String userMessage, List<Map<String, String>> messages) {
        AgentEntity agent = agentService.findEntityById(agentId);
        ChatClient client = selectClient(agent.getProvider());

        List<Message> history = buildMessages(messages);
        if (userMessage != null && !userMessage.isBlank()) {
            history.add(new UserMessage(userMessage));
        }

        return client.prompt()
                .system(systemPrompt(agent))
                .messages(history)
                .stream()
                .content();
    }

    private ChatClient selectClient(String provider) {
        return "anthropic".equalsIgnoreCase(provider) ? anthropicChatClient : openAiChatClient;
    }

    private String systemPrompt(AgentEntity agent) {
        return agent.getSystemPrompt() != null ? agent.getSystemPrompt() : "You are a helpful assistant.";
    }

    private List<Message> buildMessages(List<Map<String, String>> history) {
        List<Message> messages = new ArrayList<>();
        if (history != null) {
            for (Map<String, String> entry : history) {
                String role = entry.get("role");
                String content = entry.get("content");
                if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                } else {
                    messages.add(new UserMessage(content));
                }
            }
        }
        return messages;
    }
}
