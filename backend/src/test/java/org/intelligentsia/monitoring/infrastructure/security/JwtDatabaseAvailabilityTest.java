package org.intelligentsia.monitoring.infrastructure.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtDatabaseAvailabilityTest {
    @Test void returnsExplicitUnavailableInsteadOfLeavingStaleBusinessDataVisible() throws Exception {
        var tokens = mock(JwtTokenProvider.class);
        var users = mock(org.springframework.security.core.userdetails.UserDetailsService.class);
        when(tokens.validateToken("valid-token")).thenReturn(true);
        when(tokens.extractLogin("valid-token")).thenReturn("test");
        when(users.loadUserByUsername("test")).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("offline"));
        var filter = new JwtAuthenticationFilter(tokens, users);
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/dashboard");
        request.addHeader("Authorization", "Bearer valid-token");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(503, response.getStatus());
        assertTrue(response.getContentAsString().contains("DATABASE_UNAVAILABLE"));
        verifyNoInteractions(chain);
    }
}
