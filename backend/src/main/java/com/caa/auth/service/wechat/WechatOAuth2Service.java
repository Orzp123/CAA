package com.caa.auth.service.wechat;

import com.caa.auth.model.Account;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.AccountRepository;
import com.caa.auth.repository.TenantRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles WeChat OAuth2 login.
 *
 * <p>WeChat's token endpoint is non-standard: it returns {@code openid} and
 * {@code access_token} in the same response, so we call it directly with
 * {@link RestClient} instead of using Spring Security's OAuth2 client machinery.
 *
 * <p>First-time users have no linked {@link Account}. In that case
 * {@link #findOrCreateAccount} throws {@link RequiresProfileCompletionException}
 * and the caller must issue a short-lived temp-token via {@link #generateTempToken},
 * collect the missing profile fields, and call {@link #completeProfile}.
 */
@Service
public class WechatOAuth2Service {

    private static final String WECHAT_TOKEN_URL =
            "https://api.weixin.qq.com/sns/oauth2/access_token"
            + "?appid={appid}&secret={secret}&code={code}&grant_type=authorization_code";

    private static final String WECHAT_TENANT_CODE = "wechat";
    private static final String TEMP_KEY_PREFIX    = "wechat:temp:";
    private static final long   TEMP_TOKEN_TTL_MIN = 10L;

    private final WechatConfig         wechatConfig;
    private final AccountRepository    accountRepository;
    private final TenantRepository     tenantRepository;
    private final StringRedisTemplate  redisTemplate;
    private final RestClient           restClient;

    public WechatOAuth2Service(WechatConfig wechatConfig,
                               AccountRepository accountRepository,
                               TenantRepository tenantRepository,
                               StringRedisTemplate redisTemplate,
                               RestClient restClient) {
        this.wechatConfig      = wechatConfig;
        this.accountRepository = accountRepository;
        this.tenantRepository  = tenantRepository;
        this.redisTemplate     = redisTemplate;
        this.restClient        = restClient;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Exchanges a WeChat authorisation code for tokens.
     *
     * @param code the {@code code} from WeChat's redirect callback
     * @return deserialized WeChat token response
     */
    public WechatTokenResult exchangeCode(String code) {
        String url = WECHAT_TOKEN_URL
                .replace("{appid}",  wechatConfig.getAppid())
                .replace("{secret}", wechatConfig.getSecret())
                .replace("{code}",   code);

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(WechatTokenResult.class);
    }

    /**
     * Looks up an existing account by {@code openid}.
     *
     * @param openid   WeChat openid
     * @param unionid  WeChat unionid (stored if account is created, ignored here)
     * @return existing {@link Account}
     * @throws RequiresProfileCompletionException if no account is linked to this openid
     */
    public Account findOrCreateAccount(String openid, String unionid) {
        return accountRepository.findByWechatOpenid(openid)
                .orElseThrow(() -> new RequiresProfileCompletionException(openid));
    }

    /**
     * Stores {@code openid} in Redis under a random UUID key with a 10-minute TTL.
     *
     * @param openid WeChat openid to hold temporarily
     * @return the UUID token the frontend must present when calling
     *         {@link #completeProfile}
     */
    public String generateTempToken(String openid) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                TEMP_KEY_PREFIX + token,
                openid,
                TEMP_TOKEN_TTL_MIN,
                TimeUnit.MINUTES);
        return token;
    }

    /**
     * Completes a WeChat user's profile and persists a new {@link Account}.
     *
     * @param tempToken   UUID issued by {@link #generateTempToken}
     * @param studentNo   student / employee number
     * @param name        real name
     * @param nickname    display name
     * @param accountType role in the system
     * @return the newly saved account
     * @throws IllegalArgumentException if the temp-token has expired or is unknown
     */
    public Account completeProfile(String tempToken,
                                   String studentNo,
                                   String name,
                                   String nickname,
                                   Account.AccountType accountType) {
        String openid = redisTemplate.opsForValue().get(TEMP_KEY_PREFIX + tempToken);
        if (openid == null || openid.isBlank()) {
            throw new IllegalArgumentException(
                    "Temp token expired or invalid: " + tempToken);
        }

        Tenant wechatTenant = tenantRepository.findByCode(WECHAT_TENANT_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "WeChat tenant not found — ensure tenant with code '"
                        + WECHAT_TENANT_CODE + "' exists"));

        Account account = new Account();
        account.setTenantId(wechatTenant.getId());
        account.setStudentNo(studentNo);
        account.setName(name);
        account.setNickname(nickname);
        account.setAccountType(accountType);
        account.setWechatOpenid(openid);
        account.setStatus(Account.AccountStatus.ACTIVE);

        return accountRepository.save(account);
    }
}
