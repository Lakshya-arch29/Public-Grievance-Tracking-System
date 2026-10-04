package com.jansunwai.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request and response shapes for grievances. */
public class GrievanceDtos {

    public record GrievanceRequest(
            @NotBlank(message = "is required") @Size(min = 5, max = 150, message = "must be 5 to 150 characters") String title,
            @NotNull(message = "is required") Integer categoryId,
            @NotNull(message = "is required") Integer cityId,
            @NotBlank(message = "is required") @Size(min = 2, max = 120, message = "must be 2 to 120 characters") String area,
            @Pattern(regexp = "^(LOW|MEDIUM|HIGH)$", message = "must be LOW, MEDIUM or HIGH") String priority,
            @NotBlank(message = "is required") @Size(min = 10, max = 2000, message = "must be 10 to 2000 characters") String description) {
    }

    public record OfficerInfo(Integer userId, String fullName, String phone) {
    }

    public record CreateResponse(String trackingId, String status, String priority, OfficerInfo assignedOfficer) {
    }

    public record StatusSummary(int total, int pending, int inProgress, int resolved, int rejected) {
    }

    /** One line in the grievance list. */
    public record GrievanceRow(String trackingId, String title, String category, String city,
                               String area, String priority, String status, String createdAt) {
    }

    /** One step of the timeline. */
    public record HistoryItem(String status, String remark, String changedBy, String changedAt) {
    }

    public record GrievanceDetail(String trackingId, String title, String description, String category,
                                  String city, String area, String priority, String status,
                                  String createdAt, String updatedAt, OfficerInfo assignedOfficer,
                                  List<HistoryItem> history) {

        public GrievanceDetail withHistory(List<HistoryItem> items) {
            return new GrievanceDetail(trackingId, title, description, category, city, area, priority,
                    status, createdAt, updatedAt, assignedOfficer, items);
        }
    }
}
