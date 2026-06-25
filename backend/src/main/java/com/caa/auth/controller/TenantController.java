package com.caa.auth.controller;

import com.caa.auth.dto.*;
import com.caa.auth.model.Account;
import com.caa.auth.model.RolePermission;
import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantPermission;
import com.caa.auth.model.TenantSingleDeviceConfig;
import com.caa.auth.model.TenantSsoConfig;
import com.caa.auth.repository.TenantSingleDeviceConfigRepository;
import com.caa.auth.repository.TenantSsoConfigRepository;
import com.caa.auth.service.PermissionService;
import com.caa.auth.service.RolePermissionService;
import com.caa.auth.service.TenantPermissionService;
import com.caa.auth.service.TenantService;
import com.caa.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for tenant management.
 * All endpoints require SYSTEM_ADMIN role — enforced at the gateway level.
 */
@RestController
@RequestMapping("/admin/tenants")
public class TenantController {

    private final TenantService                      tenantService;
    private final TenantPermissionService            tenantPermissionService;
    private final RolePermissionService              rolePermissionService;
    private final PermissionService                  permissionService;
    private final TenantSsoConfigRepository          ssoConfigRepository;
    private final TenantSingleDeviceConfigRepository singleDeviceConfigRepository;

    public TenantController(TenantService tenantService,
                            TenantPermissionService tenantPermissionService,
                            RolePermissionService rolePermissionService,
                            PermissionService permissionService,
                            TenantSsoConfigRepository ssoConfigRepository,
                            TenantSingleDeviceConfigRepository singleDeviceConfigRepository) {
        this.tenantService                = tenantService;
        this.tenantPermissionService      = tenantPermissionService;
        this.rolePermissionService        = rolePermissionService;
        this.permissionService            = permissionService;
        this.ssoConfigRepository          = ssoConfigRepository;
        this.singleDeviceConfigRepository = singleDeviceConfigRepository;
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Operation(summary = "List tenants with optional status/type filter")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paged tenant list")
    @GetMapping
    public ApiResponse<Page<Tenant>> listTenants(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    Tenant.TenantStatus status,
            @RequestParam(required = false)    Tenant.TenantType   type) {
        Page<Tenant> result = tenantService.findAll(PageRequest.of(page, size), status, type);
        return ApiResponse.ok(result);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new tenant")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tenant created")
    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> createTenant(
            @Valid @RequestBody CreateTenantRequest req) {
        Tenant created = tenantService.create(req.code(), req.name(), req.type(), req.defaultLoginType());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Operation(summary = "Update tenant name, domain, or default login type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant updated")
    @PutMapping("/{tenantId}")
    public ApiResponse<Tenant> updateTenant(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantRequest req) {
        Tenant updated = tenantService.update(tenantId, req.name(), req.domain(), req.defaultLoginType());
        return ApiResponse.ok(updated);
    }

    // ── status ────────────────────────────────────────────────────────────────

    @Operation(summary = "Update tenant status (ACTIVE / INACTIVE)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/{tenantId}/status")
    public ApiResponse<Tenant> updateStatus(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantStatusRequest req) {
        Tenant updated = tenantService.updateStatus(tenantId, req.status());
        return ApiResponse.ok(updated);
    }

    // ── SSO config ────────────────────────────────────────────────────────────

    @Operation(summary = "Save or update tenant SSO (OIDC) configuration")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSO config saved")
    @PutMapping("/{tenantId}/sso-config")
    public ApiResponse<TenantSsoConfig> updateSsoConfig(
            @PathVariable String tenantId,
            @Valid @RequestBody SsoConfigRequest req) {
        TenantSsoConfig config = ssoConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantSsoConfig c = new TenantSsoConfig();
                    c.setTenantId(tenantId);
                    return c;
                });
        config.setIssuerUri(req.issuerUri());
        config.setClientId(req.clientId());
        config.setClientSecret(req.clientSecret());
        if (req.scope() != null)      config.setScope(req.scope());
        if (req.roleClaim() != null)  config.setRoleClaim(req.roleClaim());
        if (req.roleMapping() != null) config.setRoleMapping(req.roleMapping());
        return ApiResponse.ok(ssoConfigRepository.save(config));
    }

    // ── tenant permissions ────────────────────────────────────────────────────

    @Operation(summary = "Get enabled permissions for a tenant")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tenant permission list")
    @GetMapping("/{tenantId}/permissions")
    public ApiResponse<List<TenantPermission>> getTenantPermissions(@PathVariable String tenantId) {
        return ApiResponse.ok(tenantPermissionService.findByTenantId(tenantId));
    }

    @Operation(summary = "Replace all enabled permissions for a tenant")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permissions updated")
    @PutMapping("/{tenantId}/permissions")
    public ApiResponse<Void> updateTenantPermissions(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdatePermissionsRequest req) {
        tenantPermissionService.updatePermissions(tenantId, req.permissionIds());
        return ApiResponse.ok();
    }

    // ── role permissions ──────────────────────────────────────────────────────

    @Operation(summary = "Get role permissions for a tenant and account type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role permission list")
    @GetMapping("/{tenantId}/role-permissions/{accountType}")
    public ApiResponse<List<RolePermission>> getRolePermissions(
            @PathVariable String tenantId,
            @PathVariable Account.AccountType accountType) {
        return ApiResponse.ok(
                rolePermissionService.findByTenantIdAndAccountType(tenantId, accountType));
    }

    @Operation(summary = "Replace role permissions for a tenant and account type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role permissions updated")
    @PutMapping("/{tenantId}/role-permissions/{accountType}")
    public ApiResponse<Void> updateRolePermissions(
            @PathVariable String tenantId,
            @PathVariable Account.AccountType accountType,
            @Valid @RequestBody UpdatePermissionsRequest req) {
        rolePermissionService.updatePermissions(tenantId, accountType, req.permissionIds());
        return ApiResponse.ok();
    }

    // ── single-device config ──────────────────────────────────────────────────

    @Operation(summary = "Get single-device login config for a tenant")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Single-device config")
    @GetMapping("/{tenantId}/single-device-config")
    public ApiResponse<List<TenantSingleDeviceConfig>> getSingleDeviceConfig(
            @PathVariable String tenantId) {
        return ApiResponse.ok(singleDeviceConfigRepository.findAllByTenantId(tenantId));
    }

    @Operation(summary = "Save single-device login config for a tenant and account type")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Single-device config saved")
    @PutMapping("/{tenantId}/single-device-config")
    public ApiResponse<TenantSingleDeviceConfig> updateSingleDeviceConfig(
            @PathVariable String tenantId,
            @RequestParam Account.AccountType accountType,
            @RequestParam boolean enabled) {
        TenantSingleDeviceConfig config =
                singleDeviceConfigRepository
                        .findByTenantIdAndAccountType(tenantId, accountType)
                        .orElseGet(() -> {
                            TenantSingleDeviceConfig c = new TenantSingleDeviceConfig();
                            c.setTenantId(tenantId);
                            c.setAccountType(accountType);
                            return c;
                        });
        config.setEnabled(enabled);
        return ApiResponse.ok(singleDeviceConfigRepository.save(config));
    }
}
