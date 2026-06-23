package com.caa.auth.service.sso;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.model.TenantSsoConfig;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import com.caa.auth.repository.TenantSsoConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Handles OIDC/OAuth2 SSO login.
 *
 * <p>Each tenant has its own IdP configuration stored in {@code tenant_sso_configs}.
 * {@link #getClientRegistration} builds a Spring Security {@link ClientRegistration}
 * on the fly from that row — no static {@code application.yml} registration needed.
 *
 * <p>{@link #findOrCreateAccount} looks up an existing account by the IdP's stable
 * {@code sub} claim. When none exists it creates one, mapping the IdP role claim to
 * an {@link Account.AccountType} via the tenant's JSON {@code role_mapping} field.
 */
@Service
public class SsoOidcService {

    private final TenantSsoConfigRepository tenantSsoConfigRepository;
    private final TenantRepository          tenantRepository;
    private final AccountRepository         accountRepository;
    private final ObjectMapper              objectMapper;

    public SsoOidcService(TenantSsoConfigRepository tenantSsoConfigRepository,
                          TenantRepository tenantRepository,
                          AccountRepository accountRepository,
                          ObjectMapper objectMapper) {
        this.tenantSsoConfigRepository = tenantSsoConfigRepository;
        this.tenantRepository          = tenantRepository;
        this.accountRepository         = accountRepository;
        this.objectMapper              = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link ClientRegistration} from the tenant's SSO config row.
     *
     * @param tenantCode tenant's unique code (e.g. {@code "school-a"})
     * @return a ready-to-use Spring Security client registration
     * @throws IllegalArgumentException if the tenant is not found
     * @throws IllegalStateException    if no enabled SSO config exists for the tenant
     */
    public ClientRegistration getClientRegistration(String tenantCode) {
        Tenant tenant = tenantRepository.findByCode(tenantCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tenant not found: " + tenantCode));

        TenantSsoConfig config = tenantSsoConfigRepository
                .findByTenantIdAndEnabled(tenant.getId(), true)
                .orElseThrow(() -> new IllegalStateException(
                        "No enabled SSO config for tenant: " + tenantCode));

        return ClientRegistration
                .withRegistrationId(tenantCode)
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope(config.getScope().split("\\s+"))
                .authorizationUri(config.getIssuerUri() + "/protocol/openid-connect/auth")
                .tokenUri(config.getIssuerUri() + "/protocol/openid-connect/token")
                .userInfoUri(config.getIssuerUri() + "/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .issuerUri(config.getIssuerUri())
                .build();
    }

    /**
     * Thin stub for OIDC token exchange.
     *
     * <p>The actual token exchange requires a running IdP and is exercised in
     * integration tests. In unit tests this method is stubbed. The real
     * implementation would use Spring Security's {@code OAuth2AuthorizedClientManager}
     * or a direct {@code RestClient} call to the token endpoint.
     *
     * @param tenantCode  tenant code
     * @param code        authorisation code from IdP callback
     * @param redirectUri redirect URI registered with the IdP
     * @return extracted OIDC user info
     */
    public OidcUserInfo exchangeCode(String tenantCode, String code, String redirectUri) {
        // Delegate to a real OIDC token-exchange in the controller/integration layer.
        // Unit tests stub this method — see SsoOidcServiceTest.
        throw new UnsupportedOperationException(
                "exchangeCode must be integrated with the OIDC authorization flow");
    }

    /**
     * Looks up or creates an {@link Account} for the given SSO subject.
     *
     * <p>On first login the account is created with role derived from the IdP
     * role claim and the tenant's {@code role_mapping} JSON.
     *
     * @param tenantCode  tenant code
     * @param ssoSubject  IdP {@code sub} claim value
     * @param claims      full ID-token / userinfo claims map
     * @return existing or newly created account
     */
    public Account findOrCreateAccount(String tenantCode,
                                       String ssoSubject,
                                       Map<String, Object> claims) {
        Tenant tenant = tenantRepository.findByCode(tenantCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tenant not found: " + tenantCode));

        return accountRepository
                .findByTenantIdAndSsoSubject(tenant.getId(), ssoSubject)
                .orElseGet(() -> createAccount(tenant, ssoSubject, claims));
    }

    /**
     * Maps an IdP role string to an {@link Account.AccountType}.
     *
     * @param idpRole     value of the role claim from the IdP (may be null)
     * @param roleMapping map of IdP role → AccountType name (e.g. {@code {"staff":"TEACHER"}})
     * @return mapped {@link Account.AccountType}, defaulting to {@code STUDENT}
     */
    public Account.AccountType mapRole(String idpRole,
                                       Map<String, Object> roleMapping) {
        if (idpRole == null || idpRole.isBlank() || roleMapping == null) {
            return Account.AccountType.STUDENT;
        }
        Object mapped = roleMapping.get(idpRole);
        if (mapped == null) {
            return Account.AccountType.STUDENT;
        }
        try {
            return Account.AccountType.valueOf(mapped.toString());
        } catch (IllegalArgumentException e) {
            return Account.AccountType.STUDENT;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Account createAccount(Tenant tenant,
                                  String ssoSubject,
                                  Map<String, Object> claims) {
        TenantSsoConfig ssoConfig = tenantSsoConfigRepository
                .findByTenantIdAndEnabled(tenant.getId(), true)
                .orElse(null);

        Account.AccountType accountType = Account.AccountType.STUDENT;
        if (ssoConfig != null
                && ssoConfig.getRoleClaim() != null
                && ssoConfig.getRoleMapping() != null) {
            String idpRole = claimAsString(claims, ssoConfig.getRoleClaim());
            Map<String, Object> roleMappingMap = parseRoleMapping(ssoConfig.getRoleMapping());
            accountType = mapRole(idpRole, roleMappingMap);
        }

        String name      = claimAsString(claims, "name");
        String email     = claimAsString(claims, "email");
        // Use email as studentNo fallback when no explicit student number claim exists
        String studentNo = email != null ? email : ssoSubject;

        Account account = new Account();
        account.setTenantId(tenant.getId());
        account.setSsoSubject(ssoSubject);
        account.setStudentNo(studentNo);
        account.setName(name != null ? name : ssoSubject);
        account.setAccountType(accountType);
        account.setStatus(Account.AccountStatus.ACTIVE);

        return accountRepository.save(account);
    }

    private String claimAsString(Map<String, Object> claims, String key) {
        if (claims == null || key == null) return null;
        Object val = claims.get(key);
        return val != null ? val.toString() : null;
    }

    private Map<String, Object> parseRoleMapping(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
