package com.caa.workflow.dto;

import java.time.Instant;

/**
 * Response record for a WorkflowDefinition resource.
 */
public record WorkflowResponse(
        String id,
        String tenantId,
        String name,
        String description,
        String status,
        String graphJson,
        String temporalWorkflowType,
        Instant createdAt,
        Instant updatedAt
) {}
