package org.intelligentsia.monitoring.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import org.intelligentsia.monitoring.domain.auth.entity.UserScope;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserCreateRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String login;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private LocalDate dateBirth;
    private String phone;
    private String languageCode = "fr";
    private Long institutionId;
    private UserScope scope = UserScope.INSTITUTION;
    private Set<Long> roleIds;
}
