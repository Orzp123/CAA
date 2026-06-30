package com.caa.school.service;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import com.caa.common.ErrorCode;
import com.caa.school.dto.*;
import com.caa.school.exception.SchoolException;
import com.caa.school.model.BenefitPackage;
import com.caa.school.model.PromotionalSlot;
import com.caa.school.model.TenantBenefitPackage;
import com.caa.school.repository.BenefitPackageRepository;
import com.caa.school.repository.PromotionalSlotRepository;
import com.caa.school.repository.TenantBenefitPackageRepository;
import com.caa.storage.MinioStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchoolService {

    private static final int SLOT_MAX = 10;
    private static final java.util.Set<String> ALLOWED_LOGO_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png", "webp");

    // H-4 fix: 默认密码通过配置注入
    @Value("${app.account.default-password:Caa@2026}")
    private String defaultPassword;

    private final TenantRepository tenantRepository;
    private final AccountRepository accountRepository;
    private final BenefitPackageRepository benefitPackageRepository;
    private final TenantBenefitPackageRepository tenantBenefitPackageRepository;
    private final PromotionalSlotRepository promotionalSlotRepository;
    private final MinioStorageService minioStorageService;
    private final PasswordEncoder passwordEncoder;
    // C-3 fix: 使用 Jackson ObjectMapper 解析 JSON 数组
    private final ObjectMapper objectMapper;

    public SchoolService(
            TenantRepository tenantRepository,
            AccountRepository accountRepository,
            BenefitPackageRepository benefitPackageRepository,
            TenantBenefitPackageRepository tenantBenefitPackageRepository,
            PromotionalSlotRepository promotionalSlotRepository,
            MinioStorageService minioStorageService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.accountRepository = accountRepository;
        this.benefitPackageRepository = benefitPackageRepository;
        this.tenantBenefitPackageRepository = tenantBenefitPackageRepository;
        this.promotionalSlotRepository = promotionalSlotRepository;
        this.minioStorageService = minioStorageService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateSchoolResponse createSchool(CreateSchoolRequest request) {
        tenantRepository.findByCode(request.code()).ifPresent(t -> {
            throw new SchoolException(ErrorCode.SCHOOL_CODE_DUPLICATE);
        });

        BenefitPackage pkg = benefitPackageRepository.findById(request.packageId())
                .orElseThrow(() -> new SchoolException(ErrorCode.PACKAGE_NOT_FOUND));

        List<SlotRequest> slots = request.slots() != null ? request.slots() : List.of();
        if (slots.size() > SLOT_MAX) {
            throw new SchoolException(ErrorCode.SLOT_LIMIT_EXCEEDED);
        }

        Tenant tenant = new Tenant();
        tenant.setCode(request.code());
        tenant.setName(request.name());
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setDomain(request.domain());

        if (request.brand() != null) {
            tenant.setSystemNameZh(request.brand().systemNameZh());
            tenant.setSystemNameEn(request.brand().systemNameEn());
            tenant.setDescription(request.brand().description());
        }

        Tenant savedTenant = tenantRepository.save(tenant);

        String adminLoginName = "admin_" + request.code();
        Account admin = new Account();
        admin.setTenantId(savedTenant.getId());
        admin.setStudentNo(adminLoginName);
        admin.setName("管理员");
        admin.setAccountType(Account.AccountType.SCHOOL_ADMIN);
        admin.setStatus(Account.AccountStatus.ACTIVE);
        admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        accountRepository.save(admin);

        TenantBenefitPackage tbp = new TenantBenefitPackage();
        tbp.setTenantId(savedTenant.getId());
        tbp.setPackageId(pkg.getId());
        tenantBenefitPackageRepository.save(tbp);

        if (!slots.isEmpty()) {
            promotionalSlotRepository.saveAll(
                    slots.stream().map(s -> buildSlot(savedTenant.getId(), s)).toList());
        }

        return new CreateSchoolResponse(
                savedTenant.getId(),
                savedTenant.getCode(),
                savedTenant.getName(),
                adminLoginName,
                savedTenant.getStatus().name(),
                savedTenant.getCreatedAt()
        );
    }

    // N-3 fix: 校验 SCHOOL 类型; N-4 fix: 支持扩展名参数
    @Transactional(readOnly = true)
    public LogoUploadUrlResponse getLogoUploadUrl(String schoolId, String extension) {
        Tenant tenant = tenantRepository.findById(schoolId)
                .orElseThrow(() -> new EntityNotFoundException("School not found"));

        if (tenant.getType() != Tenant.TenantType.SCHOOL) {
            throw new SchoolException(ErrorCode.FORBIDDEN, "非学校租户不可上传 Logo");
        }

        // H-3 fix: extension 白名单校验，防路径穿越
        String rawExt = (extension != null && !extension.isBlank())
                ? extension.toLowerCase().replaceAll("[^a-z0-9]", "") : "jpg";
        if (!ALLOWED_LOGO_EXTENSIONS.contains(rawExt)) {
            throw new SchoolException(ErrorCode.VALIDATION_ERROR,
                    "不支持的图片格式: " + rawExt + "，合法格式: jpg, jpeg, png, webp");
        }
        String logoPath = "schools/" + schoolId + "/logo." + rawExt;
        int expiresIn = 300;
        String uploadUrl = minioStorageService.getPresignedPutUrl(logoPath, expiresIn);

        return new LogoUploadUrlResponse(uploadUrl, logoPath, expiresIn);
    }

    // M-1 fix: readOnly=true
    @Transactional(readOnly = true)
    public Page<SchoolSummaryResponse> listSchools(Pageable pageable, String name, String code) {
        // H-4 fix: 使用带 adminCount 的投影查询，消除 N+1 问题
        return tenantRepository.searchByTypeWithAdminCount(
                Tenant.TenantType.SCHOOL,
                Account.AccountType.SCHOOL_ADMIN,
                (name != null && !name.isBlank()) ? name : null,
                (code != null && !code.isBlank()) ? code : null,
                pageable
        ).map(p -> new SchoolSummaryResponse(
                p.getId(), p.getCode(), p.getName(),
                p.getStatus(), p.getAdminCount(), p.getCreatedAt()));
    }

    // M-1 fix: readOnly=true; M-4 fix: 读取套餐 defaultPermissionCodes
    @Transactional(readOnly = true)
    public SchoolDetailResponse getSchool(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School not found"));

        TenantBenefitPackage tbp = tenantBenefitPackageRepository.findByTenantId(id).orElse(null);

        List<String> permissionCodes = List.of();
        if (tbp != null) {
            BenefitPackage pkg = benefitPackageRepository.findById(tbp.getPackageId()).orElse(null);
            if (pkg != null && pkg.getDefaultPermissionCodes() != null) {
                permissionCodes = parseJsonArray(pkg.getDefaultPermissionCodes());
            }
        }

        List<SlotRequest> slots = promotionalSlotRepository
                .findAllByTenantIdOrderBySortOrderAsc(id).stream()
                .map(s -> new SlotRequest(s.getTitle(), s.getImageUrl(), s.getLinkUrl(),
                        s.getPosition().name(), s.getSortOrder()))
                .toList();

        return new SchoolDetailResponse(
                tenant.getId(), tenant.getCode(), tenant.getName(), tenant.getDomain(),
                tenant.getStatus().name(),
                new BrandInfo(tenant.getSystemNameZh(), tenant.getSystemNameEn(), tenant.getDescription()),
                tbp != null ? tbp.getPackageId() : null,
                permissionCodes, slots, tenant.getCreatedAt()
        );
    }

    // M-3 fix: 直接组装响应，不调用无事务的 getSchool()
    @Transactional
    public SchoolDetailResponse updateSchool(String id, UpdateSchoolRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School not found"));

        if (request.name() != null) tenant.setName(request.name());
        if (request.domain() != null) tenant.setDomain(request.domain());
        if (request.brand() != null) {
            tenant.setSystemNameZh(request.brand().systemNameZh());
            tenant.setSystemNameEn(request.brand().systemNameEn());
            tenant.setDescription(request.brand().description());
        }
        tenantRepository.save(tenant);

        String packageId = null;
        List<String> permissionCodes = List.of();
        if (request.packageId() != null) {
            BenefitPackage pkg = benefitPackageRepository.findById(request.packageId())
                    .orElseThrow(() -> new SchoolException(ErrorCode.PACKAGE_NOT_FOUND));
            tenantBenefitPackageRepository.deleteByTenantId(id);
            TenantBenefitPackage tbp = new TenantBenefitPackage();
            tbp.setTenantId(id);
            tbp.setPackageId(request.packageId());
            tenantBenefitPackageRepository.save(tbp);
            packageId = pkg.getId();
            if (pkg.getDefaultPermissionCodes() != null) {
                permissionCodes = parseJsonArray(pkg.getDefaultPermissionCodes());
            }
        } else {
            TenantBenefitPackage existingTbp = tenantBenefitPackageRepository.findByTenantId(id).orElse(null);
            if (existingTbp != null) packageId = existingTbp.getPackageId();
        }

        List<SlotRequest> resultSlots;
        if (request.slots() != null) {
            // M-4 fix: 运营位替换逻辑拆为独立 @Transactional 方法，减小事务粒度
            resultSlots = replacePromotionalSlots(id, request.slots());
        } else {
            resultSlots = promotionalSlotRepository.findAllByTenantIdOrderBySortOrderAsc(id).stream()
                    .map(s -> new SlotRequest(s.getTitle(), s.getImageUrl(), s.getLinkUrl(),
                            s.getPosition().name(), s.getSortOrder()))
                    .toList();
        }

        return new SchoolDetailResponse(
                tenant.getId(), tenant.getCode(), tenant.getName(), tenant.getDomain(),
                tenant.getStatus().name(),
                new BrandInfo(tenant.getSystemNameZh(), tenant.getSystemNameEn(), tenant.getDescription()),
                packageId, permissionCodes, resultSlots, tenant.getCreatedAt()
        );
    }

    // M-10 fix: 显式校验枚举，抛 SchoolException 而非依赖 IllegalArgumentException
    @Transactional
    public void updateStatus(String id, String status) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School not found"));
        Tenant.TenantStatus tenantStatus;
        try {
            tenantStatus = Tenant.TenantStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new SchoolException(ErrorCode.VALIDATION_ERROR,
                    "无效的学校状态值: " + status + "，合法值: ACTIVE, INACTIVE");
        }
        tenant.setStatus(tenantStatus);
        tenantRepository.save(tenant);
    }

    private PromotionalSlot buildSlot(String tenantId, SlotRequest s) {
        PromotionalSlot slot = new PromotionalSlot();
        slot.setTenantId(tenantId);
        slot.setTitle(s.title());
        slot.setImageUrl(s.imageUrl());
        slot.setLinkUrl(s.linkUrl());
        slot.setPosition(PromotionalSlot.SlotPosition.valueOf(s.position()));
        slot.setSortOrder(s.sortOrder());
        slot.setStatus(PromotionalSlot.SlotStatus.ACTIVE);
        return slot;
    }

    /**
     * M-4 fix: 运营位替换逻辑拆为独立 @Transactional 方法，减小事务粒度。
     * 校验数量上限 → 删除旧记录 → 批量保存新记录。
     */
    @Transactional
    public List<SlotRequest> replacePromotionalSlots(String tenantId, List<SlotRequest> slots) {
        if (slots.size() > SLOT_MAX) {
            throw new SchoolException(ErrorCode.SLOT_LIMIT_EXCEEDED);
        }
        promotionalSlotRepository.deleteAllByTenantId(tenantId);
        promotionalSlotRepository.saveAll(
                slots.stream().map(s -> buildSlot(tenantId, s)).toList());
        return slots;
    }

    /**
     * C-3 fix: 改用 Jackson ObjectMapper 解析 JSON 数组，替代手写字符串分割。
     */
    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
