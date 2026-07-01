package com.caa.school.repository;

import com.caa.school.model.TenantBenefitPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantBenefitPackageRepository extends JpaRepository<TenantBenefitPackage, String> {

    Optional<TenantBenefitPackage> findByTenantId(String tenantId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByTenantId(String tenantId);
}
