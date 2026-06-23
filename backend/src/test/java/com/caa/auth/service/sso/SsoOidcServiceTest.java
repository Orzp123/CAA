package com.caa.auth.service.sso;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantSsoConfig;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import com.caa.auth.repository.TenantSsoConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SsoOidcService}.
 * No Spring context — all dependencies mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class SsoOidcServiceTest {

    @Mock
    private TenantSsoConfigRepository tenantSsoConfigRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AccountRepository accountRepository;

    private SsoOidcService service;

    @BeforeEach
    void setUp() {
        service = new SsoOidcService(
                tenantSsoConfigRepository,
                tenantRepository,
                accountRepository,
                new ObjectMapper()
        );
    }

    // ── getClientRegistration ─────────────────────────────────────────────────

    @Test
    void getClientRegistration_returnsRegistration_forKnownTenant() {
        Tenant tenant = buildTenant("tenant-sso-1", "school-a");
        TenantSsoConfig config = buildSsoConfig("tenant-sso-1",
                "https://idp.school-a.edu/oidc",
                "client-id-abc",
                "client-secret-xyz",
                "openid profile email");

        when(tenantRepository.findByCode("school-a")).thenReturn(Optional.of(tenant));
        when(tenantSsoConfigRepository.findByTenantIdAndEnabled("tenant-sso-1", true))
                .thenReturn(Optional.of(config));

        ClientRegistration reg = service.getClientRegistration("school-a");

        assertThat(reg).isNotNull();
        assertThat(reg.getClientId()).isEqualTo("client-id-abc");
        assertThat(reg.getClientSecret()).isEqualTo("client-secret-xyz");
        assertThat(reg.getAuthorizationGrantType())
                .isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(reg.getProviderDetails().getIssuerUri())
                .isEqualTo("https://idp.school-a.edu/oidc");
    }

    @Test
    void getClientRegistration_throwsException_forUnknownTenant() {
        when(tenantRepository.findByCode("unknown-tenant")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClientRegistration("unknown-tenant"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-tenant");
    }

    @Test
    void getClientRegistration_throwsException_whenNoEnabledSsoConfig() {
        Tenant tenant = buildTenant("tenant-sso-2", "school-b");
        when(tenantRepository.findByCode("school-b")).thenReturn(Optional.of(tenant));
        when(tenantSsoConfigRepository.findByTenantIdAndEnabled("tenant-sso-2", true))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getClientRegistration("school-b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("school-b");
    }

    // ── mapRole ───────────────────────────────────────────────────────────────

    @Test
    void mapRole_returnsTeacher_forMappedIdpRole() {
        Map<String, Object> roleMapping = Map.of("teacher_role", "TEACHER");

        Account.AccountType result = service.mapRole("teacher_role", roleMapping);

        assertThat(result).isEqualTo(Account.AccountType.TEACHER);
    }

    @Test
    void mapRole_returnsStudent_asDefault_forUnknownIdpRole() {
        Map<String, Object> roleMapping = Map.of("teacher_role", "TEACHER");

        Account.AccountType result = service.mapRole("unknown_role", roleMapping);

        assertThat(result).isEqualTo(Account.AccountType.STUDENT);
    }

    @Test
    void mapRole_returnsStudent_forNullIdpRole() {
        Account.AccountType result = service.mapRole(null, Map.of());

        assertThat(result).isEqualTo(Account.AccountType.STUDENT);
    }

    // ── findOrCreateAccount ───────────────────────────────────────────────────

    @Test
    void findOrCreateAccount_returnsExistingAccount_whenSsoSubjectMatches() {
        Tenant tenant = buildTenant("tenant-sso-1", "school-a");
        Account existing = buildAccount("tenant-sso-1", "sub-12345");

        when(tenantRepository.findByCode("school-a")).thenReturn(Optional.of(tenant));
        when(accountRepository.findByTenantIdAndSsoSubject("tenant-sso-1", "sub-12345"))
                .thenReturn(Optional.of(existing));

        Map<String, Object> claims = Map.of("sub", "sub-12345", "name", "Alice");
        Account result = service.findOrCreateAccount("school-a", "sub-12345", claims);

        assertThat(result).isSameAs(existing);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void findOrCreateAccount_createsNewAccount_whenSsoSubjectNotFound() {
        Tenant tenant = buildTenant("tenant-sso-1", "school-a");
        TenantSsoConfig ssoConfig = buildSsoConfig("tenant-sso-1",
                "https://idp.school-a.edu/oidc",
                "client-id-abc", "client-secret-xyz", "openid profile email");
        // role_mapping: {"staff":"TEACHER"}
        ssoConfig.setRoleClaim("role");
        ssoConfig.setRoleMapping("{\"staff\":\"TEACHER\"}");

        when(tenantRepository.findByCode("school-a")).thenReturn(Optional.of(tenant));
        when(accountRepository.findByTenantIdAndSsoSubject("tenant-sso-1", "sub-new"))
                .thenReturn(Optional.empty());
        when(tenantSsoConfigRepository.findByTenantIdAndEnabled("tenant-sso-1", true))
                .thenReturn(Optional.of(ssoConfig));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        when(accountRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> claims = Map.of(
                "sub", "sub-new",
                "name", "Bob",
                "email", "bob@school-a.edu",
                "role", "staff"
        );

        Account result = service.findOrCreateAccount("school-a", "sub-new", claims);

        assertThat(result).isNotNull();
        Account saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-sso-1");
        assertThat(saved.getSsoSubject()).isEqualTo("sub-new");
        assertThat(saved.getName()).isEqualTo("Bob");
        assertThat(saved.getAccountType()).isEqualTo(Account.AccountType.TEACHER);
        assertThat(saved.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Tenant buildTenant(String id, String code) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setCode(code);
        t.setName(code + " School");
        t.setType(Tenant.TenantType.SCHOOL);
        t.setStatus(Tenant.TenantStatus.ACTIVE);
        return t;
    }

    private TenantSsoConfig buildSsoConfig(String tenantId, String issuerUri,
                                            String clientId, String clientSecret,
                                            String scope) {
        TenantSsoConfig c = new TenantSsoConfig();
        c.setTenantId(tenantId);
        c.setIssuerUri(issuerUri);
        c.setClientId(clientId);
        c.setClientSecret(clientSecret);
        c.setScope(scope);
        c.setEnabled(true);
        return c;
    }

    private Account buildAccount(String tenantId, String ssoSubject) {
        Account a = new Account();
        a.setTenantId(tenantId);
        a.setSsoSubject(ssoSubject);
        a.setStudentNo("SSO-001");
        a.setName("Existing User");
        a.setAccountType(Account.AccountType.STUDENT);
        a.setStatus(Account.AccountStatus.ACTIVE);
        return a;
    }
}
