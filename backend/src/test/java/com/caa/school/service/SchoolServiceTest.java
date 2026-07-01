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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SchoolService 单元测试（TDD RED 阶段）。
 * 使用 Mockito 隔离所有外部依赖。
 */
@ExtendWith(MockitoExtension.class)
class SchoolServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BenefitPackageRepository benefitPackageRepository;

    @Mock
    private TenantBenefitPackageRepository tenantBenefitPackageRepository;

    @Mock
    private PromotionalSlotRepository promotionalSlotRepository;

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private PasswordEncoder passwordEncoder;

    // C-3 fix: SchoolService 构造器需要 ObjectMapper，使用真实实例
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SchoolService schoolService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(schoolService, "defaultPassword", "Caa@2026");
    }

    // ─── createSchool ───────────────────────────────────────────────────────────

    @Test
    void createSchool_success() {
        // given
        String code = "school001";
        BenefitPackage pkg = new BenefitPackage();
        pkg.setId("pkg-1");
        pkg.setCode("basic");
        pkg.setName("基础套餐");
        pkg.setStorageGb(10);
        pkg.setMaxAgents(5);
        pkg.setDefaultPermissionCodes("[]");
        pkg.setStatus(BenefitPackage.PackageStatus.ACTIVE);

        when(tenantRepository.findByCode(code)).thenReturn(Optional.empty());
        when(benefitPackageRepository.findById("pkg-1")).thenReturn(Optional.of(pkg));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        Tenant savedTenant = new Tenant();
        savedTenant.setId("tenant-uuid-1");
        savedTenant.setCode(code);
        savedTenant.setName("测试学校");
        savedTenant.setType(Tenant.TenantType.SCHOOL);
        savedTenant.setStatus(Tenant.TenantStatus.ACTIVE);
        when(tenantRepository.save(tenantCaptor.capture())).thenReturn(savedTenant);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        Account savedAccount = new Account();
        savedAccount.setId("account-uuid-1");
        savedAccount.setStudentNo("admin_" + code);
        when(accountRepository.save(accountCaptor.capture())).thenReturn(savedAccount);

        when(tenantBenefitPackageRepository.save(any(TenantBenefitPackage.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(promotionalSlotRepository.saveAll(anyList())).thenReturn(List.of());

        CreateSchoolRequest request = new CreateSchoolRequest(
                "测试学校",
                code,
                "school001.caa.com",
                "pkg-1",
                List.of("PERM_A"),
                new BrandInfo("测试学校中文", "Test School", "描述"),
                List.of(new SlotRequest("标题", "http://img.png", "http://link.com", "HOME_TOP_BANNER", 1))
        );

        // when
        CreateSchoolResponse response = schoolService.createSchool(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.defaultAdminLoginName()).isEqualTo("admin_" + code);
        assertThat(response.code()).isEqualTo(code);
        assertThat(response.status()).isEqualTo("ACTIVE");

        // 验证 Tenant 字段
        Tenant capturedTenant = tenantCaptor.getValue();
        assertThat(capturedTenant.getType()).isEqualTo(Tenant.TenantType.SCHOOL);
        assertThat(capturedTenant.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);

        // 验证默认管理员账户
        Account capturedAccount = accountCaptor.getValue();
        assertThat(capturedAccount.getStudentNo()).isEqualTo("admin_" + code);
        assertThat(capturedAccount.getAccountType()).isEqualTo(Account.AccountType.SCHOOL_ADMIN);
        assertThat(capturedAccount.getPasswordHash()).isNotBlank();

        // 验证套餐关联保存
        verify(tenantBenefitPackageRepository).save(any(TenantBenefitPackage.class));
        // 验证运营位批量保存
        verify(promotionalSlotRepository).saveAll(anyList());
    }

    @Test
    void createSchool_codeConflict() {
        // given
        String code = "existing";
        Tenant existingTenant = new Tenant();
        existingTenant.setCode(code);
        when(tenantRepository.findByCode(code)).thenReturn(Optional.of(existingTenant));

        CreateSchoolRequest request = new CreateSchoolRequest(
                "冲突学校", code, "existing.caa.com", "pkg-1",
                List.of(), null, List.of()
        );

        // when / then
        assertThatThrownBy(() -> schoolService.createSchool(request))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SCHOOL_CODE_DUPLICATE));

        verify(tenantRepository, never()).save(any());
    }

    @Test
    void createSchool_packageNotFound() {
        // given
        String code = "newschool";
        when(tenantRepository.findByCode(code)).thenReturn(Optional.empty());
        when(benefitPackageRepository.findById("nonexistent-pkg")).thenReturn(Optional.empty());

        CreateSchoolRequest request = new CreateSchoolRequest(
                "新学校", code, "newschool.caa.com", "nonexistent-pkg",
                List.of(), null, List.of()
        );

        // when / then
        assertThatThrownBy(() -> schoolService.createSchool(request))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PACKAGE_NOT_FOUND));
    }

    @Test
    void createSchool_slotLimitExceeded() {
        // given
        String code = "slottest";
        BenefitPackage pkg = new BenefitPackage();
        pkg.setId("pkg-1");
        pkg.setDefaultPermissionCodes("[]");
        pkg.setStatus(BenefitPackage.PackageStatus.ACTIVE);

        when(tenantRepository.findByCode(code)).thenReturn(Optional.empty());
        when(benefitPackageRepository.findById("pkg-1")).thenReturn(Optional.of(pkg));

        // 构造 11 条运营位（超出限制 10）
        List<SlotRequest> slots = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(i -> new SlotRequest("标题" + i, null, null, "HOME_TOP_BANNER", i))
                .toList();

        CreateSchoolRequest request = new CreateSchoolRequest(
                "运营位测试学校", code, "slottest.caa.com", "pkg-1",
                List.of(), null, slots
        );

        // when / then
        assertThatThrownBy(() -> schoolService.createSchool(request))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SLOT_LIMIT_EXCEEDED));
    }

    // ─── C-3: parseJsonArray 使用 Jackson ───────────────────────────────────────

    @Test
    void getSchool_parseJsonArray_validJson() {
        // given: benefit_package 含合法 JSON 数组
        String schoolId = "tenant-uuid-1";
        Tenant tenant = buildTenant(schoolId, "school001");

        TenantBenefitPackage tbp = new TenantBenefitPackage();
        tbp.setTenantId(schoolId);
        tbp.setPackageId("pkg-1");

        BenefitPackage pkg = new BenefitPackage();
        pkg.setId("pkg-1");
        pkg.setDefaultPermissionCodes("[\"AGENT_READ\",\"CHAT_USE\"]");
        pkg.setStatus(BenefitPackage.PackageStatus.ACTIVE);

        when(tenantRepository.findById(schoolId)).thenReturn(Optional.of(tenant));
        when(tenantBenefitPackageRepository.findByTenantId(schoolId)).thenReturn(Optional.of(tbp));
        when(benefitPackageRepository.findById("pkg-1")).thenReturn(Optional.of(pkg));
        when(promotionalSlotRepository.findAllByTenantIdOrderBySortOrderAsc(schoolId))
                .thenReturn(List.of());

        // when
        SchoolDetailResponse resp = schoolService.getSchool(schoolId);

        // then: Jackson 正确解析出两个权限码
        assertThat(resp.permissionCodes()).containsExactly("AGENT_READ", "CHAT_USE");
    }

    @Test
    void getSchool_parseJsonArray_malformedJson_returnsEmpty() {
        // given: JSON 格式损坏
        String schoolId = "tenant-uuid-2";
        Tenant tenant = buildTenant(schoolId, "school002");

        TenantBenefitPackage tbp = new TenantBenefitPackage();
        tbp.setTenantId(schoolId);
        tbp.setPackageId("pkg-2");

        BenefitPackage pkg = new BenefitPackage();
        pkg.setId("pkg-2");
        // 损坏的 JSON（手写分割会静默出错，Jackson 会抛异常并返回空列表）
        pkg.setDefaultPermissionCodes("AGENT_READ,CHAT_USE");
        pkg.setStatus(BenefitPackage.PackageStatus.ACTIVE);

        when(tenantRepository.findById(schoolId)).thenReturn(Optional.of(tenant));
        when(tenantBenefitPackageRepository.findByTenantId(schoolId)).thenReturn(Optional.of(tbp));
        when(benefitPackageRepository.findById("pkg-2")).thenReturn(Optional.of(pkg));
        when(promotionalSlotRepository.findAllByTenantIdOrderBySortOrderAsc(schoolId))
                .thenReturn(List.of());

        // when
        SchoolDetailResponse resp = schoolService.getSchool(schoolId);

        // then: 格式损坏时返回空列表，不抛异常
        assertThat(resp.permissionCodes()).isEmpty();
    }

    // ─── M-4: replacePromotionalSlots 独立方法 ──────────────────────────────────

    @Test
    void replacePromotionalSlots_success() {
        // given
        String tenantId = "tenant-uuid-1";
        List<SlotRequest> slots = List.of(
                new SlotRequest("标题1", "http://img.png", "http://link.com", "HOME_TOP_BANNER", 1),
                new SlotRequest("标题2", null, null, "HOME_SIDEBAR", 2)
        );
        when(promotionalSlotRepository.saveAll(anyList())).thenReturn(List.of());

        // when
        List<SlotRequest> result = schoolService.replacePromotionalSlots(tenantId, slots);

        // then
        verify(promotionalSlotRepository).deleteAllByTenantId(tenantId);
        verify(promotionalSlotRepository).saveAll(anyList());
        assertThat(result).hasSize(2);
    }

    @Test
    void replacePromotionalSlots_slotLimitExceeded() {
        // given: 11 条超出上限
        String tenantId = "tenant-uuid-1";
        List<SlotRequest> slots = java.util.stream.IntStream.rangeClosed(1, 11)
                .mapToObj(i -> new SlotRequest("标题" + i, null, null, "HOME_TOP_BANNER", i))
                .toList();

        // when / then
        assertThatThrownBy(() -> schoolService.replacePromotionalSlots(tenantId, slots))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SLOT_LIMIT_EXCEEDED));
        verify(promotionalSlotRepository, never()).deleteAllByTenantId(any());
    }

    // ─── 工具方法 ────────────────────────────────────────────────────────────────

    private Tenant buildTenant(String id, String code) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setCode(code);
        t.setName("测试学校");
        t.setType(Tenant.TenantType.SCHOOL);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        return t;
    }

    // ─── getLogoUploadUrl ───────────────────────────────────────────────────────

    @Test
    void getLogoUploadUrl_success() {
        // given
        String schoolId = "tenant-uuid-1";
        Tenant tenant = new Tenant();
        tenant.setId(schoolId);
        tenant.setCode("school001");
        tenant.setType(Tenant.TenantType.SCHOOL);

        when(tenantRepository.findById(schoolId)).thenReturn(Optional.of(tenant));
        when(minioStorageService.getPresignedPutUrl(
                eq("schools/" + schoolId + "/logo.jpg"), eq(300)))
                .thenReturn("https://minio.example.com/schools/" + schoolId + "/logo.jpg?X-Amz-Signature=abc");

        // when
        LogoUploadUrlResponse response = schoolService.getLogoUploadUrl(schoolId, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.uploadUrl()).isNotBlank();
        assertThat(response.logoPath()).isEqualTo("schools/" + schoolId + "/logo.jpg");
        assertThat(response.expiresIn()).isEqualTo(300);
    }
}

