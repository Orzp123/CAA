package com.caa.auth.controller;

import com.caa.auth.dto.CreateTenantRequest;
import com.caa.auth.dto.UpdatePermissionsRequest;
import com.caa.auth.dto.UpdateTenantStatusRequest;
import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantPermission;
import com.caa.auth.model.RolePermission;
import com.caa.auth.repository.TenantSingleDeviceConfigRepository;
import com.caa.auth.repository.TenantSsoConfigRepository;
import com.caa.auth.service.PermissionService;
import com.caa.auth.service.RolePermissionService;
import com.caa.auth.service.TenantPermissionService;
import com.caa.auth.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TenantController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.config.import=",
    "spring.cloud.nacos.config.import-check.enabled=false"
})
@Import(JacksonAutoConfiguration.class)
class TenantControllerTest {

    @Autowired MockMvc       mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean TenantService                      tenantService;
    @MockitoBean TenantPermissionService            tenantPermissionService;
    @MockitoBean RolePermissionService              rolePermissionService;
    @MockitoBean PermissionService                  permissionService;
    @MockitoBean TenantSsoConfigRepository          ssoConfigRepository;
    @MockitoBean TenantSingleDeviceConfigRepository singleDeviceConfigRepository;
    @MockitoBean com.caa.auth.service.TokenService  tokenService;

    private static final String TENANT_ID = "ten-uuid-001";

    private Tenant buildTenant() {
        Tenant t = new Tenant();
        t.setId(TENANT_ID);
        t.setCode("pku");
        t.setName("北京大学");
        t.setType(Tenant.TenantType.SCHOOL);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        t.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        return t;
    }

    // ── GET /admin/tenants ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/tenants returns 200 with paged list")
    void listTenants_returns200() throws Exception {
        Page<Tenant> page = new PageImpl<>(List.of(buildTenant()));
        when(tenantService.findAll(any(Pageable.class), isNull(), isNull()))
                .thenReturn(page);

        mockMvc.perform(get("/admin/tenants")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(TENANT_ID));
    }

    // ── POST /admin/tenants ───────────────────────────────────────────────────

    @Test
    @DisplayName("POST /admin/tenants returns 201 with created tenant")
    void createTenant_returns201() throws Exception {
        when(tenantService.create(anyString(), anyString(),
                any(Tenant.TenantType.class), any(Tenant.LoginType.class)))
                .thenReturn(buildTenant());

        CreateTenantRequest req = new CreateTenantRequest(
                "pku", "北京大学", Tenant.TenantType.SCHOOL, Tenant.LoginType.PASSWORD);

        mockMvc.perform(post("/admin/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TENANT_ID));
    }

    // ── PUT /admin/tenants/{id}/status ────────────────────────────────────────

    @Test
    @DisplayName("PUT /admin/tenants/{id}/status returns 200")
    void updateStatus_returns200() throws Exception {
        when(tenantService.updateStatus(eq(TENANT_ID), eq(Tenant.TenantStatus.INACTIVE)))
                .thenReturn(buildTenant());

        UpdateTenantStatusRequest req = new UpdateTenantStatusRequest(Tenant.TenantStatus.INACTIVE);

        mockMvc.perform(put("/admin/tenants/{id}/status", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── GET /admin/tenants/{id}/permissions ───────────────────────────────────

    @Test
    @DisplayName("GET /admin/tenants/{id}/permissions returns 200 with list")
    void getTenantPermissions_returns200() throws Exception {
        TenantPermission tp = new TenantPermission();
        tp.setTenantId(TENANT_ID);
        tp.setPermissionId("perm-uuid-001");
        tp.setEnabled(true);

        when(tenantPermissionService.findByTenantId(TENANT_ID))
                .thenReturn(List.of(tp));

        mockMvc.perform(get("/admin/tenants/{id}/permissions", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].tenantId").value(TENANT_ID));
    }

    // ── PUT /admin/tenants/{id}/permissions ───────────────────────────────────

    @Test
    @DisplayName("PUT /admin/tenants/{id}/permissions returns 200")
    void updateTenantPermissions_returns200() throws Exception {
        UpdatePermissionsRequest req = new UpdatePermissionsRequest(
                List.of("perm-uuid-001", "perm-uuid-002"));

        mockMvc.perform(put("/admin/tenants/{id}/permissions", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
