package org.intelligentsia.monitoring.shared.annotation;

import java.lang.annotation.*;

/**
 * Marks a service method that applies tenant-based data filtering via {@link
 * org.intelligentsia.monitoring.infrastructure.tenant.TenantContext}.
 * The method must call {@code TenantContext.get()} to obtain the effective institution ID.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TenantAware {}
