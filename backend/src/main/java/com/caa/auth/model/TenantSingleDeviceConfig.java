package com.caa.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "tenant_single_device_configs",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_tsdc_tenant_account_type",
        columnNames = {"tenant_id", "account_type"}
    )
)
public class TenantSingleDeviceConfig {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 16)
    private Account.AccountType accountType;

    @Column(nullable = false)
    private boolean enabled;

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
    public Account.AccountType getAccountType() { return accountType; }
    public void setAccountType(Account.AccountType accountType) { this.accountType = accountType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
