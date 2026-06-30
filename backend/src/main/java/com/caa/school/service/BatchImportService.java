package com.caa.school.service;

import com.caa.auth.model.Account;
import com.caa.auth.repository.AccountRepository;
import com.caa.common.ErrorCode;
import com.caa.school.dto.BatchImportResponse;
import com.caa.school.exception.SchoolException;
import com.caa.storage.MinioStorageService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

/**
 * 批量导入账户服务。
 * 支持 .xlsx/.csv 文件，使用 Virtual Threads 解析，部分成功逻辑。
 */
@Service
public class BatchImportService {

    private static final int MAX_ROWS = 1000;
    private static final long MAX_FILE_BYTES = 10 * 1024 * 1024L;

    // H-4 fix: 默认密码通过配置注入
    @Value("${app.account.default-password:Caa@2026}")
    private String defaultPassword;

    private final AccountRepository accountRepository;
    private final MinioStorageService minioStorageService;
    private final PasswordEncoder passwordEncoder;

    public BatchImportService(AccountRepository accountRepository,
                              MinioStorageService minioStorageService,
                              PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.minioStorageService = minioStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public BatchImportResponse batchImport(String tenantId, MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() != null
                ? file.getOriginalFilename().toLowerCase() : "";

        // 1. 格式校验
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".csv")) {
            throw new SchoolException(ErrorCode.FILE_FORMAT_INVALID);
        }

        // 2. C-3 fix: 文件大小预检，防止 OOM
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new SchoolException(ErrorCode.FILE_TOO_LARGE);
        }

        // 3. 解析行数据
        List<String[]> rows = parseFile(file, filename);

        // 4. 行数校验（不含 header）
        if (rows.size() > MAX_ROWS) {
            throw new SchoolException(ErrorCode.FILE_TOO_LARGE);
        }

        // 4. 逐行处理（部分成功）
        List<ImportResult> results = new ArrayList<>();
        for (String[] row : rows) {
            results.add(processRow(tenantId, row));
        }

        // 5. 生成报告 Excel 并上传至 MinIO
        String reportUrl = generateAndUploadReport(tenantId, rows, results);

        long successCount = results.stream().filter(r -> r.success).count();
        long failureCount = results.stream().filter(r -> !r.success).count();

        return new BatchImportResponse(
                rows.size(),
                (int) successCount,
                (int) failureCount,
                reportUrl
        );
    }

    // M-6 fix: 移除 Virtual Thread 包装（同步操作加 VT 是反模式），直接调用
    private List<String[]> parseFile(MultipartFile file, String filename) throws Exception {
        if (filename.endsWith(".csv")) {
            return parseCsv(file.getInputStream());
        } else {
            return parseXlsx(file.getInputStream());
        }
    }

    private List<String[]> parseCsv(InputStream is) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new InputStreamReader(is))) {
            List<String[]> all = reader.readAll();
            // 去掉 header 行
            if (!all.isEmpty()) {
                all.remove(0);
            }
            return all;
        }
    }

    private List<String[]> parseXlsx(InputStream is) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) { firstRow = false; continue; } // 跳过 header
                String[] cells = new String[5];
                for (int i = 0; i < 5; i++) {
                    Cell cell = row.getCell(i);
                    cells[i] = cell != null ? cell.toString().trim() : "";
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    // ──────────────────── 单行处理 ────────────────────

    /**
     * 列顺序：loginName, name, accountType, secondaryRole, password
     */
    private ImportResult processRow(String tenantId, String[] row) {
        if (row.length < 2) {
            return new ImportResult(row, false, "行数据不完整");
        }

        String loginName = safeGet(row, 0);
        String name = safeGet(row, 1);
        String accountTypeStr = safeGet(row, 2);
        String secondaryRoleStr = safeGet(row, 3);
        String password = safeGet(row, 4);

        // 登录名已存在 → 失败
        if (accountRepository.findByTenantIdAndStudentNo(tenantId, loginName).isPresent()) {
            return new ImportResult(row, false, "登录名已存在");
        }

        try {
            Account.AccountType accountType = accountTypeStr.isBlank()
                    ? Account.AccountType.STUDENT
                    : Account.AccountType.valueOf(accountTypeStr.toUpperCase());

            Account.SecondaryRole secondaryRole = null;
            if (!secondaryRoleStr.isBlank()) {
                secondaryRole = Account.SecondaryRole.valueOf(secondaryRoleStr.toUpperCase());
            }

            Account account = new Account();
            account.setTenantId(tenantId);
            account.setStudentNo(loginName);
            account.setName(name);
            account.setAccountType(accountType);
            account.setSecondaryRole(secondaryRole);
            account.setStatus(Account.AccountStatus.ACTIVE);

            String rawPwd = (password != null && !password.isBlank()) ? password : defaultPassword;
            account.setPasswordHash(passwordEncoder.encode(rawPwd));

            accountRepository.save(account);
            return new ImportResult(row, true, "成功");
        } catch (IllegalArgumentException e) {
            return new ImportResult(row, false, "账户类型或第二身份值无效");
        }
    }

    // ──────────────────── 报告生成与上传 ────────────────────

    private String generateAndUploadReport(String tenantId,
                                            List<String[]> rows,
                                            List<ImportResult> results) throws Exception {
        byte[] reportBytes = buildReportExcel(rows, results);
        String objectName = "import-reports/" + tenantId + "/report-" + System.currentTimeMillis() + ".xlsx";

        minioStorageService.upload(
                objectName,
                new ByteArrayInputStream(reportBytes),
                reportBytes.length,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        return minioStorageService.getPresignedUrl(objectName, 60);
    }

    private byte[] buildReportExcel(List<String[]> rows, List<ImportResult> results) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("导入结果");

            // Header
            String[] headers = {"loginName", "name", "accountType", "secondaryRole", "password", "result", "failureReason"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data rows
            for (int i = 0; i < results.size(); i++) {
                ImportResult res = results.get(i);
                Row row = sheet.createRow(i + 1);
                String[] src = res.row;
                for (int j = 0; j < 5; j++) {
                    // 密码列屏蔽；其余字段净化 CSV 注入前缀（=,+,-,@,\t,\r）
                    String val = j == 4 ? "***" : sanitizeCell(safeGet(src, j));
                    row.createCell(j).setCellValue(val);
                }
                row.createCell(5).setCellValue(res.success ? "成功" : "失败");
                row.createCell(6).setCellValue(res.success ? "" : sanitizeCell(res.reason));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ──────────────────── 辅助类型与方法 ────────────────────

    private String safeGet(String[] arr, int idx) {
        if (arr == null || idx >= arr.length) return "";
        return arr[idx] != null ? arr[idx].trim() : "";
    }

    // H-7 fix: 净化 Excel 单元格内容，防止 CSV 注入
    private String sanitizeCell(String value) {
        if (value == null || value.isEmpty()) return "";
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@'
                || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }

    private record ImportResult(String[] row, boolean success, String reason) {}
}
