package com.caa.page.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request record for creating or updating a PageSchema.
 */
public record PageRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100)
        String name,

        @Size(max = 200)
        String path,

        @Size(max = 500)
        String description,

        @NotBlank(message = "schemaJson is required")
        String schemaJson,

        String status
) {}
