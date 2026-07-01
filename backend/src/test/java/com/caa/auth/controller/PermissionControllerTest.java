package com.caa.auth.controller;

import com.caa.auth.dto.CreatePermissionRequest;
import com.caa.auth.dto.UpdatePermissionStatusRequest;
import com.caa.auth.model.Permission;
import com.caa.auth.service.PermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.cloud.nacos.config.enabled=false",
    "spring.cloud.nacos.discovery.enabled=false",
    "spring.config.import=",
    "spring.cloud.nacos.config.import-check.enabled=false"
})
@Import(JacksonAutoConfiguration.class)
class PermissionControllerTest {

    @Autowired MockMvc      mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean PermissionService permissionService;
    @MockitoBean com.caa.auth.service.TokenService tokenService;

    private static final String PERM_ID = "perm-uuid-001";

    private Permission buildPermission() {
        Permission p = new Permission();
        p.setId(PERM_ID);
        p.setCode("AGENT_CREATE");
        p.setName("创建 Agent");
        p.setModule("agent");
        p.setAction("create");
        p.setStatus(Permission.PermissionStatus.ACTIVE);
        return p;
    }

    // ── GET /admin/permissions ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /admin/permissions returns 200 with full list")
    void listPermissions_noModule_returns200() throws Exception {
        when(permissionService.findAll()).thenReturn(List.of(buildPermission()));

        mockMvc.perform(get("/admin/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(PERM_ID));
    }

    @Test
    @DisplayName("GET /admin/permissions?module=agent returns 200 filtered list")
    void listPermissions_withModule_returns200() throws Exception {
        when(permissionService.findByModule("agent")).thenReturn(List.of(buildPermission()));

        mockMvc.perform(get("/admin/permissions").param("module", "agent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].module").value("agent"));
    }

    // ── POST /admin/permissions ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /admin/permissions returns 201 with created permission")
    void createPermission_returns201() throws Exception {
        when(permissionService.create(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(buildPermission());

        CreatePermissionRequest req = new CreatePermissionRequest(
                "AGENT_CREATE", "创建 Agent", "agent", "create");

        mockMvc.perform(post("/admin/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(PERM_ID));
    }

    // ── PUT /admin/permissions/{id}/status ────────────────────────────────────

    @Test
    @DisplayName("PUT /admin/permissions/{id}/status INACTIVE returns 200")
    void updatePermissionStatus_returns200() throws Exception {
        UpdatePermissionStatusRequest req =
                new UpdatePermissionStatusRequest(Permission.PermissionStatus.INACTIVE);

        mockMvc.perform(put("/admin/permissions/{id}/status", PERM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
