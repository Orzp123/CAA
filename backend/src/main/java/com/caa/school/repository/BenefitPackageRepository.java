package com.caa.school.repository;

import com.caa.school.model.BenefitPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenefitPackageRepository extends JpaRepository<BenefitPackage, String> {

    List<BenefitPackage> findAllByStatus(BenefitPackage.PackageStatus status);
}
