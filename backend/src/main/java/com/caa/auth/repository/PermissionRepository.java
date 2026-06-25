package com.caa.auth.repository;

import com.caa.auth.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByCode(String code);

    List<Permission> findAllByStatus(Permission.PermissionStatus status);

    List<Permission> findAllByModule(String module);
}
