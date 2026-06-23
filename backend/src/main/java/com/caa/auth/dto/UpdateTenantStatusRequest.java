package com.caa.auth.dto;

import com.caa.auth.model.Tenant;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantStatusRequest(
        @NotNull Tenant.TenantStatus status
) {}
