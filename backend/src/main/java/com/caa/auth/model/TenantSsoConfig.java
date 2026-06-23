package com.caa.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_sso_configs")
public class TenantSsoConfig {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false, unique = true)
    private String tenantId;

    @Column(name = "issuer_uri", length = 512, nullable = false)
    private String issuerUri;

    @Column(name = "client_id", length = 256, nullable = false)
    private String clientId;

    @Column(name = "client_secret", length = 512, nullable = false)
    private String clientSecret;

    @Column(length = 256, nullable = false)
    private String scope = "openid profile email";

    @Column(name = "role_claim", length = 128)
    private String roleClaim;

    @Column(name = "role_mapping", columnDefinition = "TEXT")
    private String roleMapping;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getIssuerUri() { return issuerUri; }
    public void setIssuerUri(String issuerUri) { this.issuerUri = issuerUri; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getRoleClaim() { return roleClaim; }
    public void setRoleClaim(String roleClaim) { this.roleClaim = roleClaim; }
    public String getRoleMapping() { return roleMapping; }
    public void setRoleMapping(String roleMapping) { this.roleMapping = roleMapping; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
