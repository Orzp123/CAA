package com.caa.auth.controller;

import com.caa.auth.dto.CreatePermissionRequest;
import com.caa.auth.dto.UpdatePermissionRequest;
import com.caa.auth.dto.UpdatePermissionStatusRequest;
import com.caa.auth.model.Permission;
import com.caa.auth.service.PermissionService;
import com.caa.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for permission catalogue management.
 * All endpoints require SYSTEM_ADMIN role — enforced at the gateway level.
 */
@RestController
@RequestMapping("/admin/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Operation(summary = "List all permissions, optionally filtered by module")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission list")
    @GetMapping
    public ApiResponse<List<Permission>> listPermissions(
            @RequestParam(required = false) String module) {
        List<Permission> result = (module != null && !module.isBlank())
                ? permissionService.findByModule(module)
                : permissionService.findAll();
        return ApiResponse.ok(result);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new permission")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Permission created")
    @PostMapping
    public ResponseEntity<ApiResponse<Permission>> createPermission(
            @Valid @RequestBody CreatePermissionRequest req) {
        Permission created = permissionService.create(
                req.code(), req.name(), req.module(), req.action());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    // ── update name ───────────────────────────────────────────────────────────

    @Operation(summary = "Update permission display name")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission updated")
    @PutMapping("/{id}")
    public ApiResponse<Permission> updatePermission(
            @PathVariable String id,
            @Valid @RequestBody UpdatePermissionRequest req) {
        Permission existing = permissionService.findById(id);
        Permission updated = permissionService.update(id, req.name(), existing.getStatus());
        return ApiResponse.ok(updated);
    }

    // ── update status ─────────────────────────────────────────────────────────

    @Operation(summary = "Activate or deactivate a permission")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission status updated")
    @PutMapping("/{id}/status")
    public ApiResponse<Void> updatePermissionStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdatePermissionStatusRequest req) {
        if (req.status() == Permission.PermissionStatus.INACTIVE) {
            permissionService.deactivate(id);
        } else {
            Permission existing = permissionService.findById(id);
            permissionService.update(id, existing.getName(), req.status());
        }
        return ApiResponse.ok();
    }
}
