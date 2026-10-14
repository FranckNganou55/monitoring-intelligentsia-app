package org.intelligentsia.monitoring.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserDto user;

    @Getter
    @Builder
    public static class UserDto {
        private Long id;
        private String login;
        private String email;
        private String firstName;
        private String lastName;
        private String status;
        private String languageCode;
        private String scope;
        private Long institutionId;
        private String institutionName;
        private Set<String> roles;
        private Set<String> permissions;
    }
}
