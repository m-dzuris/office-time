package com.officetime.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 60) String displayName,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String password) {
    }

    public record UserResponse(Long id, String email, String displayName) {

        static UserResponse from(AppUserPrincipal principal) {
            return new UserResponse(principal.getId(), principal.getUsername(), principal.getDisplayName());
        }
    }
}
