package org.intelligentsia.monitoring.infrastructure.tenant;

/**
 * Stores the effective institution ID for the current request thread.
 * Populated by {@link TenantFilter} after JWT authentication.
 * Null means "no institution filter" (all institutions visible).
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CTX = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Long institutionId) {
        CTX.set(institutionId);
    }

    public static Long get() {
        return CTX.get();
    }

    public static void clear() {
        CTX.remove();
    }
}
