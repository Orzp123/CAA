package com.caa.auth.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantTest {

    @Test
    void defaultStatus_isActive() {
        assertThat(new Tenant().getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
    }

    @Test
    void defaultLoginType_isPassword() {
        assertThat(new Tenant().getDefaultLoginType()).isEqualTo(Tenant.LoginType.PASSWORD);
    }

    @Test
    void prePersist_setsIdAndTimestamps() {
        Tenant tenant = buildTenant();
        tenant.prePersist();

        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getCreatedAt()).isNotNull();
        assertThat(tenant.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        Tenant tenant = buildTenant();
        tenant.setId("existing-id");
        tenant.prePersist();

        assertThat(tenant.getId()).isEqualTo("existing-id");
    }

    @Test
    void preUpdate_refreshesUpdatedAt() throws InterruptedException {
        Tenant tenant = buildTenant();
        tenant.prePersist();
        var before = tenant.getUpdatedAt();

        Thread.sleep(2);
        tenant.preUpdate();

        assertThat(tenant.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void settersAndGetters_roundTrip() {
        Tenant tenant = new Tenant();
        tenant.setCode("school-a");
        tenant.setName("学校A");
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setDomain("school-a.example.com");
        tenant.setDefaultLoginType(Tenant.LoginType.SSO);
        tenant.setStatus(Tenant.TenantStatus.INACTIVE);

        assertThat(tenant.getCode()).isEqualTo("school-a");
        assertThat(tenant.getName()).isEqualTo("学校A");
        assertThat(tenant.getType()).isEqualTo(Tenant.TenantType.SCHOOL);
        assertThat(tenant.getDomain()).isEqualTo("school-a.example.com");
        assertThat(tenant.getDefaultLoginType()).isEqualTo(Tenant.LoginType.SSO);
        assertThat(tenant.getStatus()).isEqualTo(Tenant.TenantStatus.INACTIVE);
    }

    @Test
    void allTenantTypeValues_exist() {
        assertThat(Tenant.TenantType.values())
                .containsExactlyInAnyOrder(
                        Tenant.TenantType.ADMIN,
                        Tenant.TenantType.SCHOOL,
                        Tenant.TenantType.WECHAT);
    }

    @Test
    void allLoginTypeValues_exist() {
        assertThat(Tenant.LoginType.values())
                .containsExactlyInAnyOrder(
                        Tenant.LoginType.PASSWORD,
                        Tenant.LoginType.WECHAT,
                        Tenant.LoginType.SSO);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Tenant buildTenant() {
        Tenant t = new Tenant();
        t.setCode("test");
        t.setName("测试租户");
        t.setType(Tenant.TenantType.SCHOOL);
        return t;
    }
}
