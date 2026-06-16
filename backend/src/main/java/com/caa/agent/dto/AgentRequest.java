package com.caa.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request record for creating or updating an Agent.
 */
public record AgentRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotBlank(message = "provider is required")
        String provider,

        @NotBlank(message = "model is required")
        String model,

        String systemPrompt,

        String status,

        java.util.Map<String, Object> config
) {}
