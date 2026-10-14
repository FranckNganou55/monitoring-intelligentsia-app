package org.intelligentsia.monitoring.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.intelligentsia.monitoring.domain.auth.entity.UserScope;

import java.util.Set;

@Data
public class UserUpdateRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email @NotBlank private String email;
    private String phone;
    private String languageCode;
    private Long institutionId;
    private UserScope scope;
    private Set<Long> roleIds;
}
