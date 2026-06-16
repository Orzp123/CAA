package com.caa.chat.dto;

/**
 * Response record for a blocking chat completion.
 */
public record ChatResponse(
        String agentId,
        String content,
        String model,
        String provider
) {}
