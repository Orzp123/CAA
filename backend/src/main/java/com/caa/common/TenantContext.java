package com.caa.common;

/**
 * Thread-local holder for the current request's tenant identifier.
 *
 * Virtual-thread safety: ThreadLocal is safe with virtual threads in Java 21
 * as long as values are cleared after each request (TenantFilter does this).
 * Migrate to ScopedValue once it exits preview in a future Java release.
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String get() {
        String id = TENANT.get();
        return id != null ? id : "default";
    }

    public static void clear() {
        TENANT.remove();
    }
}
