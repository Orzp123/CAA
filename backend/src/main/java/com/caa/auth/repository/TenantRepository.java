package com.caa.auth.repository;

import com.caa.auth.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, String> {

    Optional<Tenant> findByCode(String code);

    Optional<Tenant> findByDomain(String domain);

    List<Tenant> findAllByStatus(Tenant.TenantStatus status);
}
