package com.caa.school.service;

import com.caa.auth.model.Account;
import com.caa.auth.repository.AccountRepository;
import com.caa.common.ErrorCode;
import com.caa.school.dto.BatchImportResponse;
import com.caa.school.exception.SchoolException;
import com.caa.storage.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchImportServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MinioStorageService minioStorageService;

    private BatchImportService service;

    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setUp() {
        service = new BatchImportService(accountRepository, minioStorageService,
                new BCryptPasswordEncoder());
    }

    @Test
    void batchImport_partialSuccess() throws Exception {
        // given: 3行数据，第2行登录名已存在
        String csvContent = "loginName,name,accountType,secondaryRole,password\n"
                + "stu001,张三,STUDENT,,\n"
                + "stu002,李四,STUDENT,,\n"   // 重复
                + "stu003,王五,TEACHER,,\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "import.csv", "text/csv",
                csvContent.getBytes());

        // stu002 已存在
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, "stu001"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, "stu002"))
                .thenReturn(Optional.of(buildAccount("stu002")));
        when(accountRepository.findByTenantIdAndStudentNo(TENANT_ID, "stu003"))
                .thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        when(minioStorageService.upload(anyString(), any(), anyLong(), anyString()))
                .thenReturn("report/result.xlsx");
        when(minioStorageService.getPresignedUrl(anyString(), anyInt()))
                .thenReturn("https://minio/report/result.xlsx");

        // when
        BatchImportResponse resp = service.batchImport(TENANT_ID, file);

        // then
        assertThat(resp.total()).isEqualTo(3);
        assertThat(resp.successCount()).isEqualTo(2);
        assertThat(resp.failureCount()).isEqualTo(1);
        assertThat(resp.reportDownloadUrl()).isNotBlank();
    }

    @Test
    void batchImport_fileTooLarge() throws Exception {
        // given: 构造 1001 行 CSV（含header = 1002行）
        StringBuilder sb = new StringBuilder("loginName,name,accountType,secondaryRole,password\n");
        for (int i = 0; i <= 1000; i++) {
            sb.append("stu").append(i).append(",用户").append(i).append(",STUDENT,,\n");
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "import.csv", "text/csv",
                sb.toString().getBytes());

        // when / then
        assertThatThrownBy(() -> service.batchImport(TENANT_ID, file))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }

    @Test
    void batchImport_invalidFormat() throws Exception {
        // given: 不支持的格式
        MockMultipartFile file = new MockMultipartFile(
                "file", "import.txt", "text/plain",
                "some content".getBytes());

        // when / then
        assertThatThrownBy(() -> service.batchImport(TENANT_ID, file))
                .isInstanceOf(SchoolException.class)
                .satisfies(ex -> assertThat(((SchoolException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FILE_FORMAT_INVALID));
    }

    // ──────────────────── 工具方法 ────────────────────

    private Account buildAccount(String studentNo) {
        Account acc = new Account();
        acc.setId("id-" + studentNo);
        acc.setTenantId(TENANT_ID);
        acc.setStudentNo(studentNo);
        acc.setName("用户");
        acc.setAccountType(Account.AccountType.STUDENT);
        acc.setStatus(Account.AccountStatus.ACTIVE);
        return acc;
    }
}
