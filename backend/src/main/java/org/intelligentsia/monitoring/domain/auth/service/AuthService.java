package org.intelligentsia.monitoring.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.auth.dto.AuthResponse;
import org.intelligentsia.monitoring.domain.auth.dto.LoginRequest;
import org.intelligentsia.monitoring.domain.auth.dto.PasswordResetRequest;
import org.intelligentsia.monitoring.domain.auth.dto.RefreshTokenRequest;
import org.intelligentsia.monitoring.domain.auth.entity.RefreshToken;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.repository.UserRepository;
import org.intelligentsia.monitoring.domain.notification.email.EmailService;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("[AUTH-SERVICE] authenticate() appelé pour '{}'", request.getLogin());
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        User user = (User) auth.getPrincipal();
        log.info("[AUTH-SERVICE] Authentification réussie pour '{}' (status: {})", user.getLogin(), user.getStatus());

        tokenService.revokeAllUserTokens(user.getId());
        String accessToken = tokenProvider.generateAccessToken(user.getLogin());
        RefreshToken refreshToken = tokenService.createRefreshToken(user);
        log.info("[AUTH-SERVICE] Tokens générés pour '{}' — accessToken length: {}", user.getLogin(), accessToken.length());

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = tokenService.validateRefreshToken(request.getRefreshToken());
        User user = refreshToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user.getLogin());
        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(Long userId) {
        tokenService.revokeAllUserTokens(userId);
        log.info("Déconnexion utilisateur ID {}", userId);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = tokenProvider.generateAccessToken(user.getLogin());
            emailService.sendPasswordReset(user.getEmail(), user.getFirstName(), token);
        });
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (!tokenProvider.validateToken(request.getToken())) {
            throw new BusinessException("INVALID_RESET_TOKEN", "Token invalide ou expiré.", HttpStatus.BAD_REQUEST);
        }
        String login = tokenProvider.extractLogin(request.getToken());
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Utilisateur introuvable.", HttpStatus.NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        tokenService.revokeAllUserTokens(user.getId());
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roles = user.getRoles().stream().map(r -> r.getCode()).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .login(user.getLogin())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .status(user.getStatus().name())
                        .languageCode(user.getLanguageCode())
                        .scope(user.getScope() != null ? user.getScope().name() : "INSTITUTION")
                        .institutionId(user.getInstitution() != null ? user.getInstitution().getId() : null)
                        .institutionName(user.getInstitution() != null ? user.getInstitution().getName() : null)
                        .roles(roles)
                        .permissions(permissions)
                        .build())
                .build();
    }
}
