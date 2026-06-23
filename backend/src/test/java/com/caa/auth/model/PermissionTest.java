package com.caa.auth.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionTest {

    @Test
    void defaultStatus_isActive() {
        assertThat(new Permission().getStatus()).isEqualTo(Permission.PermissionStatus.ACTIVE);
    }

    @Test
    void defaultIsSystem_isFalse() {
        assertThat(new Permission().isSystem()).isFalse();
    }

    @Test
    void prePersist_setsIdAndTimestamps() {
        Permission permission = buildPermission();
        permission.prePersist();

        assertThat(permission.getId()).isNotNull();
        assertThat(permission.getCreatedAt()).isNotNull();
        assertThat(permission.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersist_doesNotOverwriteExistingId() {
        Permission permission = buildPermission();
        permission.setId("fixed-id");
        permission.prePersist();

        assertThat(permission.getId()).isEqualTo("fixed-id");
    }

    @Test
    void preUpdate_refreshesUpdatedAt() throws InterruptedException {
        Permission permission = buildPermission();
        permission.prePersist();
        var before = permission.getUpdatedAt();

        Thread.sleep(2);
        permission.preUpdate();

        assertThat(permission.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void settersAndGetters_roundTrip() {
        Permission p = new Permission();
        p.setCode("AGENT_READ");
        p.setName("Agent 查看");
        p.setModule("AGENT");
        p.setAction("READ");
        p.setStatus(Permission.PermissionStatus.INACTIVE);
        p.setSystem(true);

        assertThat(p.getCode()).isEqualTo("AGENT_READ");
        assertThat(p.getName()).isEqualTo("Agent 查看");
        assertThat(p.getModule()).isEqualTo("AGENT");
        assertThat(p.getAction()).isEqualTo("READ");
        assertThat(p.getStatus()).isEqualTo(Permission.PermissionStatus.INACTIVE);
        assertThat(p.isSystem()).isTrue();
    }

    @Test
    void allPermissionStatusValues_exist() {
        assertThat(Permission.PermissionStatus.values())
                .containsExactlyInAnyOrder(
                        Permission.PermissionStatus.ACTIVE,
                        Permission.PermissionStatus.INACTIVE);
    }

    @Test
    void systemPermission_canBeToggled() {
        Permission p = buildPermission();
        p.setSystem(true);
        assertThat(p.isSystem()).isTrue();

        p.setSystem(false);
        assertThat(p.isSystem()).isFalse();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Permission buildPermission() {
        Permission p = new Permission();
        p.setCode("AGENT_READ");
        p.setName("Agent 查看");
        p.setModule("AGENT");
        p.setAction("READ");
        return p;
    }
}
