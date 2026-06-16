package com.caa.agent.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Response record for an Agent resource.
 */
public record AgentResponse(
        String id,
        String tenantId,
        String name,
        String description,
        String provider,
        String model,
        String systemPrompt,
        String status,
        Map<String, Object> config,
        Instant createdAt,
        Instant updatedAt
) {}
