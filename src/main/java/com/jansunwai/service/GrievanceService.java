package com.jansunwai.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jansunwai.dto.GrievanceDtos.CreateResponse;
import com.jansunwai.dto.GrievanceDtos.GrievanceDetail;
import com.jansunwai.dto.GrievanceDtos.GrievanceRequest;
import com.jansunwai.dto.GrievanceDtos.GrievanceRow;
import com.jansunwai.dto.GrievanceDtos.OfficerInfo;
import com.jansunwai.dto.GrievanceDtos.StatusSummary;
import com.jansunwai.exception.ApiException;
import com.jansunwai.repository.GrievanceRepository;
import com.jansunwai.repository.GrievanceRepository.Created;
import com.jansunwai.repository.LookupRepository;

/** Business rules for citizens filing and viewing grievances. */
@Service
public class GrievanceService {

    private static final Set<String> STATUSES = Set.of("PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED");

    private final GrievanceRepository grievances;
    private final LookupRepository lookup;

    public GrievanceService(GrievanceRepository grievances, LookupRepository lookup) {
        this.grievances = grievances;
        this.lookup = lookup;
    }

    /**
     * Files a grievance. Everything below runs in ONE transaction:
     * if any step fails, nothing is saved (no half-filed grievance).
     */
    @Transactional
    public CreateResponse file(int userId, GrievanceRequest req) {
        if (!lookup.categoryExists(req.categoryId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown category");
        }
        if (!lookup.cityExists(req.cityId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown city");
        }
        String priority = req.priority() == null ? "MEDIUM" : req.priority();

        // 1. Save the grievance (always starts as PENDING) and its first history row
        Created created = grievances.insert(userId, req, priority);
        grievances.addHistory(created.grievanceId(), "PENDING", "Grievance submitted", userId);

        // 2. Auto-assign: same city + category first, then same city, otherwise leave unassigned
        OfficerInfo officer = grievances.findLeastLoadedOfficer(req.cityId(), req.categoryId())
                .or(() -> grievances.findLeastLoadedOfficerInCity(req.cityId()))
                .orElse(null);

        if (officer != null) {
            grievances.assign(created.grievanceId(), officer.userId());
            grievances.addHistory(created.grievanceId(), "PENDING",
                    "Assigned to " + officer.fullName() + " (automatic)", null);
        }

        OfficerInfo shown = officer == null ? null : new OfficerInfo(officer.userId(), officer.fullName(), null);
        return new CreateResponse(created.trackingId(), "PENDING", priority, shown);
    }

    public StatusSummary summary(int userId) {
        return grievances.summaryForCitizen(userId);
    }

    public List<GrievanceRow> list(int userId, String status) {
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown status");
        }
        return grievances.listForCitizen(userId, status == null || status.isBlank() ? null : status);
    }

    /** 404 both when the id does not exist and when it belongs to another citizen. */
    public GrievanceDetail detail(int userId, String trackingId) {
        return grievances.findForCitizen(userId, trackingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grievance not found"));
    }
}
