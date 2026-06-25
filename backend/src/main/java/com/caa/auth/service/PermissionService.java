package com.caa.auth.service;

import com.caa.auth.exception.ResourceNotFoundException;
import com.caa.auth.model.Permission;
import com.caa.auth.repository.PermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;

    public PermissionService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public List<Permission> findAll() {
        return permissionRepository.findAll();
    }

    public List<Permission> findByModule(String module) {
        return permissionRepository.findAllByModule(module);
    }

    public Permission findById(String id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
    }

    @Transactional
    public Permission create(String code, String name, String module, String action) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setModule(module);
        permission.setAction(action);
        permission.setStatus(Permission.PermissionStatus.ACTIVE);
        return permissionRepository.save(permission);
    }

    @Transactional
    public Permission update(String id, String name, Permission.PermissionStatus status) {
        Permission permission = findById(id);
        if (permission.isSystem()) {
            throw new IllegalArgumentException("Cannot update a system permission");
        }
        permission.setName(name);
        permission.setStatus(status);
        return permissionRepository.save(permission);
    }

    @Transactional
    public void deactivate(String id) {
        Permission permission = findById(id);
        if (permission.isSystem()) {
            throw new IllegalArgumentException("Cannot deactivate a system permission");
        }
        permission.setStatus(Permission.PermissionStatus.INACTIVE);
        permissionRepository.save(permission);
    }
}
