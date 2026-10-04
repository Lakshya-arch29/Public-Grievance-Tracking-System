package com.jansunwai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response shapes for sign-up and login. */
public class AuthDtos {

    public record RegisterRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "is too long") String fullName,
            @NotBlank(message = "is required")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "must be 4-30 characters: letters, digits, underscore")
            String username,
            @NotBlank(message = "is required") @Email(message = "must be a valid email") @Size(max = 120) String email,
            @NotBlank(message = "is required") @Pattern(regexp = "^\\d{10}$", message = "must be exactly 10 digits") String phone,
            @NotBlank(message = "is required") @Size(min = 8, max = 72, message = "must be at least 8 characters") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "is required") String username,
            @NotBlank(message = "is required") String password,
            @NotBlank(message = "is required") String role) {
    }

    public record RegisterResponse(int userId, String username, String role) {
    }

    public record UserInfo(int userId, String fullName, String username, String role) {
    }

    public record LoginResponse(String token, String expiresAt, UserInfo user) {
    }
}
