package com.caa.auth.dto;

import com.caa.auth.model.Tenant;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 128) String name,
        @Size(max = 256) String domain,
        Tenant.LoginType defaultLoginType
) {}
