package com.jansunwai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response shapes for admin operations. */
public class AdminDtos {

    public record CreateOfficerRequest(
            @NotBlank(message = "is required") @Size(max = 100, message = "is too long") String fullName,
            @NotBlank(message = "is required")
            @Pattern(regexp = "^[A-Za-z0-9_]{4,30}$", message = "must be 4-30 characters: letters, digits, underscore")
            String username,
            @NotBlank(message = "is required") @Email(message = "must be a valid email") @Size(max = 120) String email,
            @NotBlank(message = "is required") @Pattern(regexp = "^\\d{10}$", message = "must be exactly 10 digits") String phone,
            @NotBlank(message = "is required") @Size(min = 8, max = 72, message = "must be at least 8 characters") String password,
            @NotNull(message = "is required") Integer cityId) {
    }

    public record OfficerRow(int userId, String fullName, String username, String email,
                             String city, boolean active, int pending, int resolved) {
    }

    public record DashboardStats(int total, int pending, int inProgress, int resolved, int rejected,
                                 int overdue, int unassigned) {
    }

    public record AdminGrievanceRow(String trackingId, String title, String category, String city,
                                    String area, String priority, String status, String officerName,
                                    String officerPhone, String officerEmail,
                                    String citizenName, String createdAt) {
    }

    public record AssignRequest(@NotNull(message = "is required") Integer officerId) {
    }

}
