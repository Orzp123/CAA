package com.caa.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SsoConfigRequest(
        @NotBlank @Size(max = 512) String issuerUri,
        @NotBlank @Size(max = 256) String clientId,
        @NotBlank @Size(max = 512) String clientSecret,
        @Size(max = 256)           String scope,
        @Size(max = 128)           String roleClaim,
                                   String roleMapping
) {}
