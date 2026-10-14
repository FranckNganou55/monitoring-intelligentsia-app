package org.intelligentsia.monitoring.infrastructure.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs after {@link org.intelligentsia.monitoring.infrastructure.security.JwtAuthenticationFilter}.
 * Reads the authenticated user's scope and sets {@link TenantContext} for the request:
 * <ul>
 *   <li>INSTITUTION scope → fixed to user's own institution ID</li>
 *   <li>GLOBAL / ADMIN scope → reads optional {@code ?institutionId=} query param</li>
 * </ul>
 * Always clears the context in the finally block to prevent ThreadLocal leaks.
 */
@Slf4j
@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
                if (user.getScope() == UserScope.INSTITUTION) {
                    Long institutionId = user.getInstitution() != null ? user.getInstitution().getId() : null;
                    TenantContext.set(institutionId);
                } else {
                    String param = request.getParameter("institutionId");
                    TenantContext.set(StringUtils.hasText(param) ? Long.parseLong(param) : null);
                }
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
