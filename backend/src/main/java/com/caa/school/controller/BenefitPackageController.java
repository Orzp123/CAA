package com.caa.school.controller;

import com.caa.common.ApiResponse;
import com.caa.school.model.BenefitPackage;
import com.caa.school.repository.BenefitPackageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','SCHOOL_ADMIN')")
public class BenefitPackageController {

    private final BenefitPackageRepository benefitPackageRepository;

    public BenefitPackageController(BenefitPackageRepository benefitPackageRepository) {
        this.benefitPackageRepository = benefitPackageRepository;
    }

    @Operation(summary = "获取可用权益套餐列表")
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "套餐列表"))
    @GetMapping("/benefit-packages")
    public ApiResponse<List<BenefitPackage>> listBenefitPackages() {
        return ApiResponse.ok(benefitPackageRepository.findAllByStatus(BenefitPackage.PackageStatus.ACTIVE));
    }
}
