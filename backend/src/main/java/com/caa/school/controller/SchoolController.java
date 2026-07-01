package com.caa.school.controller;

import com.caa.school.dto.*;
import com.caa.school.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "学校管理", description = "学校租户 CRUD 及运营位管理")
@RestController
@RequestMapping("/api/v1/schools")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @Operation(summary = "创建学校", description = "创建新学校租户，同时初始化默认管理员账户、套餐关联和运营位")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误或 code 重复/套餐不存在/运营位超限")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public com.caa.common.ApiResponse<CreateSchoolResponse> createSchool(
            @Valid @RequestBody CreateSchoolRequest request) {
        return com.caa.common.ApiResponse.ok(schoolService.createSchool(request));
    }

    @Operation(summary = "学校列表", description = "分页查询学校列表，支持按名称/编码模糊搜索")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public com.caa.common.ApiResponse<Page<SchoolSummaryResponse>> listSchools(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code) {
        return com.caa.common.ApiResponse.ok(schoolService.listSchools(pageable, name, code));
    }

    @Operation(summary = "获取学校详情", description = "按 ID 获取学校完整信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "学校不存在")
    })
    @GetMapping("/{id}")
    public com.caa.common.ApiResponse<SchoolDetailResponse> getSchool(@PathVariable String id) {
        return com.caa.common.ApiResponse.ok(schoolService.getSchool(id));
    }

    @Operation(summary = "更新学校信息", description = "更新学校名称、域名、品牌信息、套餐和运营位")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "套餐不存在或运营位超限"),
            @ApiResponse(responseCode = "404", description = "学校不存在")
    })
    @PutMapping("/{id}")
    public com.caa.common.ApiResponse<SchoolDetailResponse> updateSchool(
            @PathVariable String id,
            @Valid @RequestBody UpdateSchoolRequest request) {
        return com.caa.common.ApiResponse.ok(schoolService.updateSchool(id, request));
    }

    @Operation(summary = "更新学校状态", description = "启用或禁用学校租户（ACTIVE/INACTIVE）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "状态更新成功"),
            @ApiResponse(responseCode = "404", description = "学校不存在")
    })
    @PatchMapping("/{id}/status")
    public com.caa.common.ApiResponse<Void> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateStatusRequest request) {
        schoolService.updateStatus(id, request.status());
        return com.caa.common.ApiResponse.ok();
    }

    @Operation(summary = "获取 Logo 上传预签名 URL", description = "生成 MinIO PUT 预签名 URL，客户端直传 Logo 文件")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "URL 生成成功"),
            @ApiResponse(responseCode = "404", description = "学校不存在")
    })
    @GetMapping("/{id}/logo-upload-url")
    public com.caa.common.ApiResponse<LogoUploadUrlResponse> getLogoUploadUrl(
            @PathVariable String id,
            @RequestParam(required = false) String extension) {
        return com.caa.common.ApiResponse.ok(schoolService.getLogoUploadUrl(id, extension));
    }
}
