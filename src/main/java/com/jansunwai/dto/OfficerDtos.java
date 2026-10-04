package com.jansunwai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response shapes for officer operations. */
public class OfficerDtos {

    public record OfficerDashboardStats(int assigned, int pending, int inProgress, int resolved, int rejected) {
    }

    public record OfficerGrievanceRow(String trackingId, String title, String category, String city,
                                      String area, String priority, String status, String citizenName,
                                      String createdAt) {
    }

    public record OfficerUpdateRequest(
            @NotBlank(message = "is required")
            @Pattern(regexp = "^(IN_PROGRESS|RESOLVED|REJECTED)$", message = "must be IN_PROGRESS, RESOLVED or REJECTED")
            String status,
            @NotBlank(message = "is required")
            @Size(min = 5, max = 500, message = "must be 5 to 500 characters")
            String remark) {
    }
}
