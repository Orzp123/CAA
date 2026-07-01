package com.caa.school.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "benefit_packages")
public class BenefitPackage {

    public enum PackageStatus { ACTIVE, INACTIVE }

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 64, nullable = false, unique = true)
    private String code;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(name = "storage_gb", nullable = false)
    private int storageGb;

    @Column(name = "max_agents", nullable = false)
    private int maxAgents;

    @Column(name = "default_permission_codes", nullable = false, columnDefinition = "JSON")
    private String defaultPermissionCodes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PackageStatus status = PackageStatus.ACTIVE;

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
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getStorageGb() { return storageGb; }
    public void setStorageGb(int storageGb) { this.storageGb = storageGb; }
    public int getMaxAgents() { return maxAgents; }
    public void setMaxAgents(int maxAgents) { this.maxAgents = maxAgents; }
    public String getDefaultPermissionCodes() { return defaultPermissionCodes; }
    public void setDefaultPermissionCodes(String v) { this.defaultPermissionCodes = v; }
    public PackageStatus getStatus() { return status; }
    public void setStatus(PackageStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
