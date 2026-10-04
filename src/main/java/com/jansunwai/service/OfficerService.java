package com.jansunwai.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jansunwai.dto.GrievanceDtos.GrievanceDetail;
import com.jansunwai.dto.OfficerDtos.OfficerDashboardStats;
import com.jansunwai.dto.OfficerDtos.OfficerGrievanceRow;
import com.jansunwai.dto.OfficerDtos.OfficerUpdateRequest;
import com.jansunwai.exception.ApiException;
import com.jansunwai.repository.GrievanceRepository;

/** Business rules for officer operations. */
@Service
public class OfficerService {

    private final GrievanceRepository grievances;

    public OfficerService(GrievanceRepository grievances) {
        this.grievances = grievances;
    }

    public OfficerDashboardStats dashboardStats(int officerId) {
        return grievances.officerDashboardStats(officerId);
    }

    public List<OfficerGrievanceRow> listGrievances(int officerId, String status, String priority, String search) {
        return grievances.listForOfficer(officerId, status, priority, search);
    }

    public GrievanceDetail grievanceDetail(int officerId, String trackingId) {
        return grievances.findForOfficer(officerId, trackingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grievance not found"));
    }

    @Transactional
    public void updateGrievanceStatus(int officerId, String trackingId, OfficerUpdateRequest req) {
        GrievanceDetail detail = grievances.findForOfficer(officerId, trackingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grievance not found"));

        // Officers cannot reopen resolved/rejected grievances
        if (detail.status().equals("RESOLVED") || detail.status().equals("REJECTED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This grievance is already " + detail.status().toLowerCase());
        }

        int grievanceId = grievances.findGrievanceIdByTrackingId(trackingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grievance not found"));

        grievances.updateStatus(trackingId, req.status());
        grievances.addHistory(grievanceId, req.status(), req.remark(), officerId);
    }
}
