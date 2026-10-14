package org.intelligentsia.monitoring.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.dto.UserCreateRequest;
import org.intelligentsia.monitoring.domain.auth.dto.UserDto;
import org.intelligentsia.monitoring.domain.auth.dto.UserUpdateRequest;
import org.intelligentsia.monitoring.domain.auth.entity.User;
import org.intelligentsia.monitoring.domain.auth.entity.UserStatus;
import org.intelligentsia.monitoring.domain.auth.service.UserService;
import org.intelligentsia.monitoring.shared.annotation.Auditable;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.intelligentsia.monitoring.shared.dto.FilterRequest;
import org.intelligentsia.monitoring.shared.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Utilisateurs")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Lister les utilisateurs")
    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> list(FilterRequest filter) {
        // TenantFilter has already set TenantContext — UserService.findAll() reads it directly
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll(filter)));
    }

    @Operation(summary = "Obtenir un utilisateur par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResponse<UserDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }

    @Operation(summary = "Créer un utilisateur")
    @PostMapping
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Auditable(action = "CREATE", entityType = "USER")
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(userService.create(request)));
    }

    @Operation(summary = "Mettre à jour un utilisateur")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Auditable(action = "UPDATE", entityType = "USER")
    public ResponseEntity<ApiResponse<UserDto>> update(@PathVariable Long id,
                                                        @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.update(id, request)));
    }

    @Operation(summary = "Activer / désactiver un utilisateur")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    @Auditable(action = "UPDATE", entityType = "USER")
    public ResponseEntity<ApiResponse<UserDto>> updateStatus(@PathVariable Long id,
                                                              @RequestParam UserStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateStatus(id, status)));
    }

    @Operation(summary = "Supprimer un utilisateur")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @Auditable(action = "DELETE", entityType = "USER")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur supprimé."));
    }

    @Operation(summary = "Profil de l'utilisateur courant")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(user.getId())));
    }
}
