package org.intelligentsia.monitoring.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.*;
import org.intelligentsia.monitoring.domain.auth.repository.*;
import org.intelligentsia.monitoring.domain.notification.inapp.service.SseEmitterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final SseEmitterService sse;

    @Transactional("transactionManager")
    public Role create(String code, String name, String description, Set<String> requested) {
        if (roles.findByCode(code).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code de rôle existe déjà.");
        Role role = new Role(code, name.trim());
        role.setDescription(description);
        role.setPermissions(resolve(requested));
        return roles.saveAndFlush(role);
    }

    @Transactional("transactionManager")
    public Role updatePermissions(Long id, Set<String> requested) {
        Role role = roles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rôle introuvable."));
        if ("ROLE_ADMIN".equals(role.getCode())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Le rôle Administrateur est protégé.");
        role.setPermissions(resolve(requested));
        roles.saveAndFlush(role);
        // Clients must read the committed permissions, never an uncommitted update.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { sse.permissionsChanged(); }
        });
        return role;
    }

    private Set<Permission> resolve(Set<String> requested) {
        Set<Permission> found = new HashSet<>();
        for (String code : requested) found.add(permissions.findByCode(code).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Permission inconnue : " + code)));
        return found;
    }
}
