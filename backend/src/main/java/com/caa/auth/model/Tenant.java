package com.caa.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    public enum TenantType { ADMIN, SCHOOL, WECHAT }
    public enum TenantStatus { ACTIVE, INACTIVE }
    public enum LoginType { PASSWORD, WECHAT, SSO }

    @Id
    @Column(length = 36)
    private String id;

    @Column(length = 64, nullable = false, unique = true)
    private String code;

    @Column(length = 128, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TenantType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(length = 256)
    private String domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_login_type", nullable = false, length = 16)
    private LoginType defaultLoginType = LoginType.PASSWORD;

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
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TenantType getType() { return type; }
    public void setType(TenantType type) { this.type = type; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public LoginType getDefaultLoginType() { return defaultLoginType; }
    public void setDefaultLoginType(LoginType defaultLoginType) { this.defaultLoginType = defaultLoginType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
