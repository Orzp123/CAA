package com.caa.auth.service;

import com.caa.auth.exception.ResourceNotFoundException;
import com.caa.auth.model.Permission;
import com.caa.auth.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Permission activePermission;
    private Permission systemPermission;

    @BeforeEach
    void setUp() {
        activePermission = new Permission();
        activePermission.setId("perm-1");
        activePermission.setCode("USER_READ");
        activePermission.setName("Read User");
        activePermission.setModule("user");
        activePermission.setAction("read");
        activePermission.setStatus(Permission.PermissionStatus.ACTIVE);
        activePermission.setSystem(false);

        systemPermission = new Permission();
        systemPermission.setId("perm-sys");
        systemPermission.setCode("SYS_ADMIN");
        systemPermission.setName("System Admin");
        systemPermission.setModule("system");
        systemPermission.setAction("admin");
        systemPermission.setStatus(Permission.PermissionStatus.ACTIVE);
        systemPermission.setSystem(true);
    }

    @Test
    void findAll_returnsAllPermissions() {
        when(permissionRepository.findAll()).thenReturn(List.of(activePermission, systemPermission));

        List<Permission> result = permissionService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(activePermission, systemPermission);
    }

    @Test
    void findByModule_returnsFilteredList() {
        when(permissionRepository.findAllByModule("user")).thenReturn(List.of(activePermission));

        List<Permission> result = permissionService.findByModule("user");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getModule()).isEqualTo("user");
    }

    @Test
    void findById_returnsPermission_whenExists() {
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(activePermission));

        Permission result = permissionService.findById("perm-1");

        assertThat(result.getId()).isEqualTo("perm-1");
        assertThat(result.getCode()).isEqualTo("USER_READ");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenMissing() {
        when(permissionRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.findById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void create_savesNewPermissionWithActiveStatus() {
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        Permission result = permissionService.create("USER_WRITE", "Write User", "user", "write");

        assertThat(result.getCode()).isEqualTo("USER_WRITE");
        assertThat(result.getName()).isEqualTo("Write User");
        assertThat(result.getModule()).isEqualTo("user");
        assertThat(result.getAction()).isEqualTo("write");
        assertThat(result.getStatus()).isEqualTo(Permission.PermissionStatus.ACTIVE);
        verify(permissionRepository).save(any(Permission.class));
    }

    @Test
    void deactivate_setsStatusInactive() {
        when(permissionRepository.findById("perm-1")).thenReturn(Optional.of(activePermission));
        when(permissionRepository.save(any(Permission.class))).thenAnswer(inv -> inv.getArgument(0));

        permissionService.deactivate("perm-1");

        assertThat(activePermission.getStatus()).isEqualTo(Permission.PermissionStatus.INACTIVE);
        verify(permissionRepository).save(activePermission);
    }

    @Test
    void deactivate_throwsIllegalArgumentException_whenPermissionIsSystem() {
        when(permissionRepository.findById("perm-sys")).thenReturn(Optional.of(systemPermission));

        assertThatThrownBy(() -> permissionService.deactivate("perm-sys"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("system");
    }
}
