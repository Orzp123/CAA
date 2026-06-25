package com.caa.auth.repository;

import com.caa.auth.model.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PermissionRepositoryTest {

    @Autowired
    PermissionRepository permissionRepository;

    private Permission buildPermission(String code, String module, String action) {
        Permission p = new Permission();
        p.setCode(code);
        p.setName(module + ":" + action);
        p.setModule(module);
        p.setAction(action);
        p.setStatus(Permission.PermissionStatus.ACTIVE);
        p.setSystem(false);
        return p;
    }

    @Test
    @DisplayName("findByCode returns permission when code matches")
    void findByCode_found() {
        permissionRepository.save(buildPermission("agent:create", "agent", "create"));
        Optional<Permission> result = permissionRepository.findByCode("agent:create");
        assertThat(result).isPresent();
        assertThat(result.get().getModule()).isEqualTo("agent");
    }

    @Test
    @DisplayName("findByCode returns empty when code missing")
    void findByCode_notFound() {
        assertThat(permissionRepository.findByCode("no:such")).isEmpty();
    }

    @Test
    @DisplayName("findAllByStatus returns only ACTIVE permissions")
    void findAllByStatus_active() {
        permissionRepository.save(buildPermission("agent:create", "agent", "create"));
        Permission inactive = buildPermission("agent:delete", "agent", "delete");
        inactive.setStatus(Permission.PermissionStatus.INACTIVE);
        permissionRepository.save(inactive);

        List<Permission> active = permissionRepository.findAllByStatus(Permission.PermissionStatus.ACTIVE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getCode()).isEqualTo("agent:create");
    }

    @Test
    @DisplayName("findAllByModule returns permissions for the given module")
    void findAllByModule() {
        permissionRepository.save(buildPermission("agent:create", "agent", "create"));
        permissionRepository.save(buildPermission("agent:read", "agent", "read"));
        permissionRepository.save(buildPermission("workflow:run", "workflow", "run"));

        List<Permission> agentPerms = permissionRepository.findAllByModule("agent");
        assertThat(agentPerms).hasSize(2);
        assertThat(agentPerms).extracting(Permission::getModule).containsOnly("agent");
    }

    @Test
    @DisplayName("findAllByModule returns empty when module has no permissions")
    void findAllByModule_empty() {
        assertThat(permissionRepository.findAllByModule("nonexistent")).isEmpty();
    }
}
