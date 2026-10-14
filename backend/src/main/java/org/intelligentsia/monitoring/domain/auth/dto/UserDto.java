package org.intelligentsia.monitoring.domain.auth.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String login;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateBirth;
    private String phone;
    private String status;
    private String languageCode;
    private Long institutionId;
    private String institutionName;
    private String scope;
    private Set<String> roles;
    private Set<String> permissions;
    private Instant passwordExpiresAt;
    private Instant accountExpiresAt;
    private Instant createdAt;
    private Instant updatedAt;
}
