package org.intelligentsia.monitoring.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.intelligentsia.monitoring.domain.auth.entity.Role;
import org.intelligentsia.monitoring.domain.auth.repository.RoleRepository;
import org.intelligentsia.monitoring.shared.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.intelligentsia.monitoring.domain.auth.repository.PermissionRepository;
import org.intelligentsia.monitoring.domain.auth.service.RoleService;
import org.intelligentsia.monitoring.shared.annotation.Auditable;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleService roleService;

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PermissionDto>> permissions() {
        return ApiResponse.ok(permissionRepository.findAll().stream().map(p -> new PermissionDto(p.getCode(), p.getName(), p.getResource(), p.getAction())).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "CREATE", entityType = "ROLE")
    public ApiResponse<RoleDto> create(@Valid @RequestBody CreateRoleRequest request) {
        return ApiResponse.ok(RoleDto.from(roleService.create(request.code(), request.name(), request.description(), request.permissions())));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Auditable(action = "UPDATE", entityType = "ROLE")
    public ApiResponse<RoleDto> update(@PathVariable Long id, @Valid @RequestBody PermissionsRequest request) {
        return ApiResponse.ok(RoleDto.from(roleService.updatePermissions(id, request.permissions())));
    }

    public record PermissionDto(String code, String name, String resource, String action) {}
    public record CreateRoleRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "ROLE_[A-Z][A-Z0-9_]*") String code,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description,
        @NotNull Set<@NotBlank String> permissions) {}
    public record PermissionsRequest(@NotNull Set<@NotBlank String> permissions) {}

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ApiResponse<List<RoleDto>> list() {
        List<RoleDto> roles = roleRepository.findAll().stream()
                .map(RoleDto::from)
                .toList();
        return ApiResponse.ok(roles);
    }

    public record RoleDto(Long id, String code, String name, String description, Set<String> permissions) {
        private static RoleDto from(Role role) {
            return new RoleDto(
                    role.getId(),
                    role.getCode(),
                    role.getName(),
                    role.getDescription(),
                    role.getPermissions().stream().map(permission -> permission.getCode()).collect(java.util.stream.Collectors.toSet())
            );
        }
    }
}
