package com.caa.auth.repository;

import com.caa.auth.model.Account.AccountType;
import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantSingleDeviceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TenantSingleDeviceConfigRepositoryTest {

    @Autowired
    private TenantSingleDeviceConfigRepository repository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setCode("school-001");
        tenant.setName("Test School");
        tenant.setType(Tenant.TenantType.SCHOOL);
        tenant.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        tenantRepository.save(tenant);
    }

    @Test
    void saveAndFindByTenantIdAndAccountType() {
        TenantSingleDeviceConfig config = new TenantSingleDeviceConfig();
        config.setTenantId(tenant.getId());
        config.setAccountType(AccountType.STUDENT);
        config.setEnabled(true);

        repository.save(config);

        Optional<TenantSingleDeviceConfig> found =
                repository.findByTenantIdAndAccountType(tenant.getId(), AccountType.STUDENT);
        assertThat(found).isPresent();
        assertThat(found.get().isEnabled()).isTrue();
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void findAllByTenantId() {
        TenantSingleDeviceConfig c1 = new TenantSingleDeviceConfig();
        c1.setTenantId(tenant.getId());
        c1.setAccountType(AccountType.TEACHER);
        c1.setEnabled(true);

        TenantSingleDeviceConfig c2 = new TenantSingleDeviceConfig();
        c2.setTenantId(tenant.getId());
        c2.setAccountType(AccountType.STUDENT);
        c2.setEnabled(false);

        repository.save(c1);
        repository.save(c2);

        List<TenantSingleDeviceConfig> results = repository.findAllByTenantId(tenant.getId());
        assertThat(results).hasSize(2);
    }

    @Test
    void findGlobalConfig_nullTenantIdAndNullAccountType() {
        TenantSingleDeviceConfig global = new TenantSingleDeviceConfig();
        global.setTenantId(null);
        global.setAccountType(null);
        global.setEnabled(true);

        repository.save(global);

        Optional<TenantSingleDeviceConfig> found =
                repository.findByTenantIdIsNullAndAccountTypeIsNull();
        assertThat(found).isPresent();
        assertThat(found.get().isEnabled()).isTrue();
    }

    @Test
    void prePersistGeneratesUuid() {
        TenantSingleDeviceConfig config = new TenantSingleDeviceConfig();
        config.setTenantId(tenant.getId());
        config.setAccountType(AccountType.SCHOOL_ADMIN);
        config.setEnabled(false);

        TenantSingleDeviceConfig saved = repository.save(config);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).hasSize(36);
    }

    @Test
    void findByTenantIdAndAccountType_notFound() {
        Optional<TenantSingleDeviceConfig> result =
                repository.findByTenantIdAndAccountType(tenant.getId(), AccountType.SYSTEM_ADMIN);
        assertThat(result).isEmpty();
    }

    @Test
    void tenantLevelConfig_nullAccountType() {
        TenantSingleDeviceConfig config = new TenantSingleDeviceConfig();
        config.setTenantId(tenant.getId());
        config.setAccountType(null);
        config.setEnabled(true);

        repository.save(config);

        List<TenantSingleDeviceConfig> results = repository.findAllByTenantId(tenant.getId());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAccountType()).isNull();
    }
}
