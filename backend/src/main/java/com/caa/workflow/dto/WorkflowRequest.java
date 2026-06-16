package com.caa.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request record for creating or updating a WorkflowDefinition.
 */
public record WorkflowRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        String status,

        String graphJson,

        String temporalWorkflowType
) {}
