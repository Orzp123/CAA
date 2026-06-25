package com.caa.auth.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_accounts_tenant_student_no",
               columnNames = {"tenant_id", "student_no"}))
public class Account {

    public enum AccountType { SYSTEM_ADMIN, SCHOOL_ADMIN, TEACHER, STUDENT }
    public enum AccountStatus { ACTIVE, DISABLED, LOCKED }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tenant_id", length = 36, nullable = false)
    private String tenantId;

    @Column(name = "student_no", length = 64, nullable = false)
    private String studentNo;

    @Column(length = 128, nullable = false)
    private String name;

    @Column(length = 128)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 16)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "password_hash", length = 256)
    private String passwordHash;

    @Column(name = "wechat_openid", length = 128)
    private String wechatOpenid;

    @Column(name = "wechat_unionid", length = 128)
    private String wechatUnionid;

    @Column(name = "sso_subject", length = 256)
    private String ssoSubject;

    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

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
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getWechatOpenid() { return wechatOpenid; }
    public void setWechatOpenid(String wechatOpenid) { this.wechatOpenid = wechatOpenid; }
    public String getWechatUnionid() { return wechatUnionid; }
    public void setWechatUnionid(String wechatUnionid) { this.wechatUnionid = wechatUnionid; }
    public String getSsoSubject() { return ssoSubject; }
    public void setSsoSubject(String ssoSubject) { this.ssoSubject = ssoSubject; }
    public int getLoginFailCount() { return loginFailCount; }
    public void setLoginFailCount(int loginFailCount) { this.loginFailCount = loginFailCount; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
