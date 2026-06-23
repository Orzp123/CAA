package com.caa.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "tenant_login_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_tenant_login_type",
        columnNames = {"tenant_id", "login_type"}
    )
)
public class TenantLoginConfig {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 16)
    private Tenant.LoginType loginType;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

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
    public Tenant.LoginType getLoginType() { return loginType; }
    public void setLoginType(Tenant.LoginType loginType) { this.loginType = loginType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
