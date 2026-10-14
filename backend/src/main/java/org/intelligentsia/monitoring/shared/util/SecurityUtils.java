package org.intelligentsia.monitoring.shared.util;

import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.infrastructure.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        if (auth.getPrincipal() instanceof User user) return Optional.of(user);
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    public static Optional<String> getCurrentUserLogin() {
        return getCurrentUser().map(User::getLogin);
    }

    public static Optional<Long> getCurrentUserInstitutionId() {
        return getCurrentUser()
                .map(User::getInstitution)
                .map(i -> i != null ? i.getId() : null);
    }

    public static Optional<UserScope> getCurrentScope() {
        return getCurrentUser().map(User::getScope);
    }

    /**
     * Returns the effective institution ID for the current request, as set by TenantFilter.
     * Null means "no institution filter" (all institutions visible — GLOBAL/ADMIN with no ?institutionId=).
     */
    public static Long resolveInstitutionId() {
        return TenantContext.get();
    }

    public static boolean isAdmin() {
        return getCurrentUser()
                .map(u -> u.getScope() == UserScope.ADMIN)
                .orElse(false);
    }

    public static boolean isInstitutionScoped() {
        return getCurrentUser()
                .map(User::isInstitutionScoped)
                .orElse(false);
    }
}
