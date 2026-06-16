package com.caa.page.dto;

import java.time.Instant;

/**
 * Response record for a PageSchema resource.
 */
public record PageResponse(
        String id,
        String tenantId,
        String name,
        String path,
        String description,
        String schemaJson,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
