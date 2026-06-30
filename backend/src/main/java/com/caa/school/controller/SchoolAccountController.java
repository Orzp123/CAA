package com.caa.school.controller;

import com.caa.auth.model.Account;
import com.caa.common.ApiResponse;
import com.caa.school.dto.*;
import com.caa.school.service.BatchImportService;
import com.caa.school.service.SchoolAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "学校账户管理", description = "管理学校租户下的账户（增删改查、批量导入）")
@RestController
@RequestMapping("/api/v1/schools/{schoolId}/accounts")
@PreAuthorize("hasAnyRole('SCHOOL_ADMIN','SYSTEM_ADMIN')")
public class SchoolAccountController {

    private final SchoolAccountService accountService;
    private final BatchImportService batchImportService;

    public SchoolAccountController(SchoolAccountService accountService,
                                   BatchImportService batchImportService) {
        this.accountService = accountService;
        this.batchImportService = batchImportService;
    }

    @Operation(summary = "创建账户", description = "在指定学校下创建新账户")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "创建成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "登录名冲突或字段校验失败"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权操作")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @PathVariable String schoolId,
            @Valid @RequestBody CreateAccountRequest request) {
        AccountResponse resp = accountService.createAccount(schoolId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(resp));
    }

    @Operation(summary = "查询账户列表", description = "支持按登录名/账户类型/姓名筛选，分页返回")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AccountResponse>>> listAccounts(
            @PathVariable String schoolId,
            @RequestParam(required = false) String loginName,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Account.AccountType type = null;
        if (accountType != null && !accountType.isBlank()) {
            type = Account.AccountType.valueOf(accountType.toUpperCase());
        }

        int safeSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, safeSize);
        Page<AccountResponse> result = accountService.listAccounts(
                schoolId, loginName, type, name, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "更新账户信息", description = "更新姓名/昵称/邮箱/手机，不可修改账户类型")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "字段校验失败"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "账户不存在")
    })
    @PutMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable String schoolId,
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountRequest request) {
        AccountResponse resp = accountService.updateAccount(schoolId, accountId, request);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @Operation(summary = "重置密码", description = "重置指定账户密码，password 为空时使用默认密码")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "重置成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "账户不存在")
    })
    @PatchMapping("/{accountId}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable String schoolId,
            @PathVariable String accountId,
            @Valid @RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(schoolId, accountId, request.password());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "更新账户状态", description = "将账户状态设为 ACTIVE/DISABLED")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "账户不存在")
    })
    @PatchMapping("/{accountId}/status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable String schoolId,
            @PathVariable String accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        Account.AccountStatus status = Account.AccountStatus.valueOf(request.status().toUpperCase());
        accountService.updateStatus(schoolId, accountId, status);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "删除账户", description = "删除指定账户，不可删除自身")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "不可删除自身"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "账户不存在")
    })
    @DeleteMapping("/{accountId}")
    public ResponseEntity<Void> deleteAccount(
            @PathVariable String schoolId,
            @PathVariable String accountId) {
        String currentAccountId = SecurityContextHolder.getContext().getAuthentication().getName();
        accountService.deleteAccount(schoolId, accountId, currentAccountId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "批量导入账户", description = "上传 .xlsx 或 .csv 文件批量导入账户，支持部分成功")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "导入完成（含部分成功）"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "文件格式不支持或超过行数限制")
    })
    @PostMapping("/batch-import")
    public ResponseEntity<ApiResponse<BatchImportResponse>> batchImport(
            @PathVariable String schoolId,
            @RequestParam("file") MultipartFile file) throws Exception {
        BatchImportResponse resp = batchImportService.batchImport(schoolId, file);
        return ResponseEntity.ok(ApiResponse.ok(resp));
    }

    @Operation(summary = "批量更新账户状态", description = "批量将账户设为指定状态")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "批量更新成功")
    })
    @PatchMapping("/batch-status")
    public ResponseEntity<ApiResponse<Void>> batchUpdateStatus(
            @PathVariable String schoolId,
            @Valid @RequestBody BatchStatusRequest request) {
        Account.AccountStatus status = Account.AccountStatus.valueOf(request.status().toUpperCase());
        accountService.batchUpdateStatus(schoolId, request.accountIds(), status);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "批量删除账户", description = "批量删除账户，自动跳过当前用户自身")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "批量删除完成")
    })
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDelete(
            @PathVariable String schoolId,
            @RequestBody BatchDeleteRequest request) {
        String currentAccountId = SecurityContextHolder.getContext().getAuthentication().getName();
        accountService.batchDelete(schoolId, request.accountIds(), currentAccountId);
        return ResponseEntity.noContent().build();
    }
}
