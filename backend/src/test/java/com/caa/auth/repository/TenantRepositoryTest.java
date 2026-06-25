package com.caa.auth.repository;

import com.caa.auth.model.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TenantRepositoryTest {

    @Autowired
    TenantRepository tenantRepository;

    private Tenant buildTenant(String code, String name, Tenant.TenantType type) {
        Tenant t = new Tenant();
        t.setCode(code);
        t.setName(name);
        t.setType(type);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        t.setDefaultLoginType(Tenant.LoginType.PASSWORD);
        return t;
    }

    @Test
    @DisplayName("findByCode returns tenant when code matches")
    void findByCode_found() {
        tenantRepository.save(buildTenant("pku", "北京大学", Tenant.TenantType.SCHOOL));
        Optional<Tenant> result = tenantRepository.findByCode("pku");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("北京大学");
    }

    @Test
    @DisplayName("findByCode returns empty when code missing")
    void findByCode_notFound() {
        assertThat(tenantRepository.findByCode("no-such")).isEmpty();
    }

    @Test
    @DisplayName("findByDomain returns tenant when domain matches")
    void findByDomain_found() {
        Tenant t = buildTenant("tsinghua", "清华大学", Tenant.TenantType.SCHOOL);
        t.setDomain("tsinghua.caa.example.com");
        tenantRepository.save(t);
        Optional<Tenant> result = tenantRepository.findByDomain("tsinghua.caa.example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("tsinghua");
    }

    @Test
    @DisplayName("findAllByStatus returns only matching tenants")
    void findAllByStatus() {
        tenantRepository.save(buildTenant("a", "学校A", Tenant.TenantType.SCHOOL));
        Tenant inactive = buildTenant("b", "学校B", Tenant.TenantType.SCHOOL);
        inactive.setStatus(Tenant.TenantStatus.INACTIVE);
        tenantRepository.save(inactive);
        var active = tenantRepository.findAllByStatus(Tenant.TenantStatus.ACTIVE);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getCode()).isEqualTo("a");
    }
}
