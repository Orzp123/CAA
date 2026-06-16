package com.caa.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Request record for a chat completion.
 * messages is a list of {role, content} maps matching the Spring AI convention.
 */
public record ChatRequest(
        @NotNull(message = "messages are required")
        List<Map<String, String>> messages,

        String systemPromptOverride
) {}
