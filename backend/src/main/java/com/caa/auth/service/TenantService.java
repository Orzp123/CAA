package com.caa.auth.service;

import com.caa.auth.exception.ResourceNotFoundException;
import com.caa.auth.model.Tenant;
import com.caa.auth.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages tenant lifecycle.
 *
 * <p>When a tenant is set to {@link Tenant.TenantStatus#INACTIVE}, the gateway rejects all
 * subsequent requests via {@code TenantFilter} which checks tenant status from the Redis cache.
 * This service evicts the cache entries so the filter picks up the new status immediately.
 *
 * <p>Redis keys evicted on deactivation:
 * <ul>
 *   <li>{@code tenant:info:{tenantId}}
 *   <li>{@code tenant:domain:{domain}} — only when domain is set
 * </ul>
 */
@Service
public class TenantService {

    private static final String INFO_KEY_PREFIX   = "tenant:info:";
    private static final String DOMAIN_KEY_PREFIX = "tenant:domain:";

    private final TenantRepository    tenantRepository;
    private final StringRedisTemplate redisTemplate;

    public TenantService(TenantRepository tenantRepository,
                         StringRedisTemplate redisTemplate) {
        this.tenantRepository = tenantRepository;
        this.redisTemplate    = redisTemplate;
    }

    // ── lookups ───────────────────────────────────────────────────────────────

    /**
     * @throws ResourceNotFoundException if no tenant with the given code exists
     */
    public Tenant findByCode(String code) {
        return tenantRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + code));
    }

    /**
     * @throws ResourceNotFoundException if no tenant with the given id exists
     */
    public Tenant findById(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + id));
    }

    /**
     * Returns a page of tenants, optionally filtered by status and/or type.
     * Null filter values are ignored.
     */
    public Page<Tenant> findAll(Pageable pageable,
                                Tenant.TenantStatus status,
                                Tenant.TenantType type) {
        // Post-filter in memory; a Specification query can replace this if scale demands it.
        Page<Tenant> page = tenantRepository.findAll(pageable);
        if (status == null && type == null) {
            return page;
        }
        java.util.List<Tenant> filtered = page.getContent().stream()
                .filter(t -> (status == null || t.getStatus() == status)
                          && (type   == null || t.getType()   == type))
                .collect(java.util.stream.Collectors.toList());
        return new PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    // ── mutations ─────────────────────────────────────────────────────────────

    /**
     * Creates a new tenant with {@link Tenant.TenantStatus#ACTIVE} status.
     */
    @Transactional
    public Tenant create(String code,
                         String name,
                         Tenant.TenantType type,
                         Tenant.LoginType defaultLoginType) {
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(name);
        tenant.setType(type);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setDefaultLoginType(defaultLoginType);
        return tenantRepository.save(tenant);
    }

    /**
     * Updates mutable fields of an existing tenant.
     *
     * @throws ResourceNotFoundException if the tenant does not exist
     */
    @Transactional
    public Tenant update(String id,
                         String name,
                         String domain,
                         Tenant.LoginType defaultLoginType) {
        Tenant tenant = findById(id);
        if (name != null)             tenant.setName(name);
        if (domain != null)           tenant.setDomain(domain);
        if (defaultLoginType != null) tenant.setDefaultLoginType(defaultLoginType);
        return tenantRepository.save(tenant);
    }

    /**
     * Updates the status of a tenant.
     *
     * <p>When switching to {@link Tenant.TenantStatus#INACTIVE}, this method:
     * <ol>
     *   <li>Refuses to deactivate an {@link Tenant.TenantType#ADMIN} tenant.
     *   <li>Evicts the tenant's Redis cache entries so the gateway filter reacts immediately.
     * </ol>
     *
     * @throws ResourceNotFoundException  if the tenant does not exist
     * @throws IllegalArgumentException   if attempting to deactivate the ADMIN tenant
     */
    @Transactional
    public Tenant updateStatus(String id, Tenant.TenantStatus status) {
        Tenant tenant = findById(id);

        if (status == Tenant.TenantStatus.INACTIVE
                && tenant.getType() == Tenant.TenantType.ADMIN) {
            throw new IllegalArgumentException("Cannot deactivate the ADMIN tenant");
        }

        tenant.setStatus(status);
        Tenant saved = tenantRepository.save(tenant);

        if (status == Tenant.TenantStatus.INACTIVE) {
            evictCache(saved);
        }

        return saved;
    }

    // ── internal helpers ──────────────────────────────────────────────────────

    private void evictCache(Tenant tenant) {
        redisTemplate.delete(INFO_KEY_PREFIX + tenant.getId());
        if (tenant.getDomain() != null && !tenant.getDomain().isBlank()) {
            redisTemplate.delete(DOMAIN_KEY_PREFIX + tenant.getDomain());
        }
    }
}
