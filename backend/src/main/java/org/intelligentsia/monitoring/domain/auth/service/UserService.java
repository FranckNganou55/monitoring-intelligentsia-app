package org.intelligentsia.monitoring.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.intelligentsia.monitoring.domain.auth.dto.UserCreateRequest;
import org.intelligentsia.monitoring.domain.auth.dto.UserDto;
import org.intelligentsia.monitoring.domain.auth.dto.UserUpdateRequest;
import org.intelligentsia.monitoring.domain.auth.entity.PasswordHistory;
import org.intelligentsia.monitoring.domain.auth.entity.Role;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;
import org.intelligentsia.monitoring.domain.auth.entity.UserStatus;
import org.intelligentsia.monitoring.domain.auth.repository.PasswordHistoryRepository;
import org.intelligentsia.monitoring.domain.auth.repository.RoleRepository;
import org.intelligentsia.monitoring.domain.auth.repository.UserRepository;
import org.intelligentsia.monitoring.domain.configuration.institution.entity.Institution;
import org.intelligentsia.monitoring.domain.configuration.institution.repository.InstitutionRepository;
import org.intelligentsia.monitoring.domain.notification.email.EmailService;
import org.intelligentsia.monitoring.infrastructure.exception.BusinessException;
import org.intelligentsia.monitoring.infrastructure.exception.ResourceNotFoundException;
import org.intelligentsia.monitoring.infrastructure.tenant.TenantContext;
import org.intelligentsia.monitoring.shared.annotation.TenantAware;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @TenantAware
    @Transactional(readOnly = true)
    public PageResponse<UserDto> findAll(FilterRequest filter) {
        Long institutionId = TenantContext.get();

        return PageResponse.of(
                userRepository.findWithFilters(
                    filter.getSearch(),
                    null,
                    institutionId,
                    filter.toPageable()
                ).map(this::toDto)
        );

    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return toDto(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id)));
    }

    @Transactional
    public UserDto create(UserCreateRequest request) {
        if (userRepository.existsByLogin(request.getLogin())) {
            throw new BusinessException("LOGIN_TAKEN", "Ce login est déjà utilisé.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_TAKEN", "Cet email est déjà utilisé.");
        }

        User user = new User();
        user.setLogin(request.getLogin());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDateBirth(request.getDateBirth());
        user.setPhone(request.getPhone());
        user.setLanguageCode(request.getLanguageCode() != null ? request.getLanguageCode() : "fr");
        if (request.getScope() != null) user.setScope(request.getScope());

        if (request.getInstitutionId() != null) {
            Institution inst = institutionRepository.findById(request.getInstitutionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution", request.getInstitutionId()));
            user.setInstitution(inst);
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoleIds() != null) {
            request.getRoleIds().forEach(rid -> roles.add(
                    roleRepository.findById(rid).orElseThrow(() -> new ResourceNotFoundException("Rôle", rid))));
        } else {
            roleRepository.findByCode("ROLE_OPERATOR").ifPresent(roles::add);
        }
        user.setRoles(roles);
        normalizeAdministratorAccess(user);

        User saved = userRepository.save(user);
        passwordHistoryRepository.save(new PasswordHistory(saved, saved.getPasswordHash()));

        emailService.sendWelcome(saved.getEmail(), saved.getFirstName());
        log.info("Utilisateur '{}' créé.", saved.getLogin());
        return toDto(saved);
    }

    @Transactional
    public UserDto update(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getLanguageCode() != null) user.setLanguageCode(request.getLanguageCode());
        if (request.getScope() != null) user.setScope(request.getScope());

        if (request.getInstitutionId() != null) {
            Institution inst = institutionRepository.findById(request.getInstitutionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution", request.getInstitutionId()));
            user.setInstitution(inst);
        } else if (request.getInstitutionId() == null && request.getScope() == UserScope.GLOBAL) {
            user.setInstitution(null);
        }

        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>();
            request.getRoleIds().forEach(rid -> roles.add(
                    roleRepository.findById(rid).orElseThrow(() -> new ResourceNotFoundException("Rôle", rid))));
            user.setRoles(roles);
        }

        normalizeAdministratorAccess(user);

        log.info("Utilisateur ID {} mis à jour.", id);
        return toDto(userRepository.save(user));
    }

    private void normalizeAdministratorAccess(User user) {
        boolean hasAdminRole = user.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getCode()));
        boolean hasManagerRole = user.getRoles().stream().anyMatch(role -> "ROLE_MANAGER".equals(role.getCode()));
        boolean hasOperatorRole = user.getRoles().stream().anyMatch(role -> "ROLE_OPERATOR".equals(role.getCode()));
        if (!hasAdminRole && !hasManagerRole && !hasOperatorRole && !user.getRoles().isEmpty()) {
            if (user.getInstitution() == null) throw new BusinessException("INSTITUTION_REQUIRED", "Une institution est requise pour un rôle personnalisé.");
            user.setScope(UserScope.INSTITUTION);
            return;
        }
        // A selected built-in role is authoritative. Scope is only a fallback
        // for older API clients that created a user without assigning a role.
        String canonicalRole = hasAdminRole
                ? "ROLE_ADMIN"
                : hasManagerRole
                    ? "ROLE_MANAGER"
                    : hasOperatorRole
                        ? "ROLE_OPERATOR"
                        : user.getScope() == UserScope.ADMIN
                            ? "ROLE_ADMIN"
                            : user.getScope() == UserScope.GLOBAL ? "ROLE_MANAGER" : "ROLE_OPERATOR";
        Role role = roleRepository.findByCode(canonicalRole)
                .orElseThrow(() -> new BusinessException("ROLE_MISSING", "Le rôle " + canonicalRole + " est introuvable."));

        // These three built-in roles are mutually exclusive. Choosing one role
        // also establishes the matching institution scope.
        user.setRoles(new HashSet<>(Set.of(role)));
        if ("ROLE_ADMIN".equals(canonicalRole)) {
            user.setScope(UserScope.ADMIN);
            user.setInstitution(null);
        } else if ("ROLE_MANAGER".equals(canonicalRole)) {
            user.setScope(UserScope.GLOBAL);
            user.setInstitution(null);
        } else {
            user.setScope(UserScope.INSTITUTION);
        }
    }

    @Transactional
    public UserDto updateStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        user.setStatus(status);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", id);
        }
        userRepository.deleteById(id);
        log.info("Utilisateur ID {} supprimé.", id);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setLogin(user.getLogin());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDateBirth(user.getDateBirth());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus().name());
        dto.setLanguageCode(user.getLanguageCode());
        dto.setScope(user.getScope().name());
        dto.setPasswordExpiresAt(user.getPasswordExpiresAt());
        dto.setAccountExpiresAt(user.getAccountExpiresAt());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        if (user.getInstitution() != null) {
            dto.setInstitutionId(user.getInstitution().getId());
            dto.setInstitutionName(user.getInstitution().getName());
        }
        dto.setRoles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()));
        dto.setPermissions(user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet()));
        return dto;
    }
}
