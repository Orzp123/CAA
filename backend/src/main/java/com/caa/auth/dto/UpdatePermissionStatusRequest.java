package com.caa.auth.dto;

import com.caa.auth.model.Permission;
import jakarta.validation.constraints.NotNull;

public record UpdatePermissionStatusRequest(
        @NotNull Permission.PermissionStatus status
) {}
