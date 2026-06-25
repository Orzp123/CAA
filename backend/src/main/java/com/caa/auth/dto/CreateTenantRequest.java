package com.caa.auth.dto;

import com.caa.auth.model.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 64)  String code,
        @NotBlank @Size(max = 128) String name,
        @NotNull  Tenant.TenantType type,
        @NotNull  Tenant.LoginType defaultLoginType
) {}
