package org.intelligentsia.monitoring.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            String login = tokenProvider.extractLogin(token);

            UserDetails userDetails;
            try { userDetails = userDetailsService.loadUserByUsername(login); }
            catch (RuntimeException error) {
                if (!org.intelligentsia.monitoring.infrastructure.exception.DatabaseAvailability.unavailable(error)) throw error;
                response.setStatus(503);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"code\":\"DATABASE_UNAVAILABLE\",\"message\":\"Base de donnees indisponible.\"}");
                return;
            }
            var auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null, 
                userDetails.getAuthorities()
            );

            auth.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );
            
            SecurityContextHolder.getContext().setAuthentication(auth);

            log.warn(
                "[SECURITY DEBUG] method={}, uri={}, login={}, authorities={}",
                request.getMethod(),
                request.getRequestURI(),
                login,
                userDetails.getAuthorities()
            );
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // SSE : EventSource ne peut pas envoyer de headers — token passé en query param
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }
}
