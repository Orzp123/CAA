package com.caa.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePermissionRequest(
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 64)  String module,
        @NotBlank @Size(max = 64)  String action
) {}
