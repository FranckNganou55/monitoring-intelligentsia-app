package org.intelligentsia.monitoring.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.RefreshToken;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.repository.RefreshTokenRepository;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plusMillis(refreshExpiryMs));
        return refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String tokenValue) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN",
                        "Token de rafraîchissement invalide.", HttpStatus.UNAUTHORIZED));

        if (token.isRevoked() || token.isExpired()) {
            throw new BusinessException("EXPIRED_REFRESH_TOKEN",
                    "Token de rafraîchissement expiré.", HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Transactional
    public int revokeAllTokens() {
        return refreshTokenRepository.revokeAll();
    }
}
