package org.intelligentsia.monitoring.domain.auth.service;

import org.junit.jupiter.api.Test;
import org.intelligentsia.monitoring.domain.auth.entity.*;
import org.intelligentsia.monitoring.domain.auth.repository.*;
import org.intelligentsia.monitoring.domain.notification.inapp.service.SseEmitterService;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleServiceTest {
    RoleRepository roles = mock(RoleRepository.class);
    PermissionRepository permissions = mock(PermissionRepository.class);
    SseEmitterService sse = mock(SseEmitterService.class);
    RoleService service = new RoleService(roles, permissions, sse);

    @Test void replacementRemovesOldPermissionsAndNotifiesOnlyAfterCommit() {
        Role role = new Role("ROLE_OPERATOR", "Opérateur");
        Permission old = new Permission(); old.setCode("ATM_READ");
        Permission next = new Permission(); next.setCode("POS_READ");
        role.setPermissions(new HashSet<>(Set.of(old)));
        when(roles.findById(2L)).thenReturn(Optional.of(role));
        when(permissions.findByCode("POS_READ")).thenReturn(Optional.of(next));
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.updatePermissions(2L, Set.of("POS_READ"));
            assertEquals(Set.of(next), role.getPermissions());
            verifyNoInteractions(sse);
            TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCommit());
            verify(sse).permissionsChanged();
        } finally { TransactionSynchronizationManager.clearSynchronization(); }
    }
    @Test void administratorCannotLosePermissions() {
        when(roles.findById(1L)).thenReturn(Optional.of(new Role("ROLE_ADMIN", "Administrateur")));
        assertThrows(ResponseStatusException.class, () -> service.updatePermissions(1L, Set.of()));
        verify(roles, never()).saveAndFlush(any());
    }
    @Test void unknownPermissionDoesNotModifyRole() {
        Role role = new Role("ROLE_OPERATOR", "Opérateur");
        Permission old = new Permission(); role.getPermissions().add(old);
        when(roles.findById(2L)).thenReturn(Optional.of(role));
        when(permissions.findByCode("INVALID")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.updatePermissions(2L, Set.of("INVALID")));
        assertEquals(Set.of(old), role.getPermissions());
        verifyNoInteractions(sse);
    }
    @Test void duplicateCodeIsRejected() {
        when(roles.findByCode("ROLE_OPERATOR")).thenReturn(Optional.of(new Role()));
        assertThrows(ResponseStatusException.class, () -> service.create("ROLE_OPERATOR", "Duplicate", null, Set.of()));
        verify(roles, never()).saveAndFlush(any());
    }
    @Test void customRoleUsesOnlyRequestedPermissions() {
        when(roles.findByCode("ROLE_CUSTOM")).thenReturn(Optional.empty());
        Permission p = new Permission(); p.setCode("ATM_READ");
        when(permissions.findByCode("ATM_READ")).thenReturn(Optional.of(p));
        when(roles.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        Role created = service.create("ROLE_CUSTOM", "Superviseur", "Lecture", Set.of("ATM_READ"));
        assertEquals("ROLE_CUSTOM", created.getCode());
        assertEquals(Set.of(p), created.getPermissions());
    }
}
