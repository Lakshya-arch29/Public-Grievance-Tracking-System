package com.jansunwai.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jansunwai.dto.AdminDtos.AdminGrievanceRow;
import com.jansunwai.dto.AdminDtos.CreateOfficerRequest;
import com.jansunwai.dto.AdminDtos.DashboardStats;
import com.jansunwai.dto.AdminDtos.OfficerRow;
import com.jansunwai.dto.AuthDtos.RegisterResponse;
import com.jansunwai.exception.ApiException;
import com.jansunwai.repository.GrievanceRepository;
import com.jansunwai.repository.LookupRepository;
import com.jansunwai.repository.UserRepository;

/** Business rules for admin operations. */
@Service
public class AdminService {

    private final UserRepository users;
    private final GrievanceRepository grievances;
    private final LookupRepository lookup;
    private final PasswordEncoder encoder;

    public AdminService(UserRepository users, GrievanceRepository grievances,
                        LookupRepository lookup, PasswordEncoder encoder) {
        this.users = users;
        this.grievances = grievances;
        this.lookup = lookup;
        this.encoder = encoder;
    }

    public DashboardStats dashboardStats() {
        return grievances.adminDashboardStats();
    }

    @Transactional
    public RegisterResponse createOfficer(CreateOfficerRequest req) {
        String username = req.username().trim().toLowerCase();
        String email = req.email().trim().toLowerCase();

        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (!lookup.cityExists(req.cityId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown city");
        }

        int id = users.insertOfficer(req.fullName().trim(), username, email,
                req.phone(), encoder.encode(req.password()), req.cityId());
        
        // Auto-assign any pending unassigned grievances in this city to the new officer
        grievances.assignUnassignedInCity(req.cityId(), id);
        
        return new RegisterResponse(id, username, "OFFICER");
    }

    public List<OfficerRow> listOfficers(String search, Boolean active) {
        return users.listOfficers(search, active);
    }

    public void toggleOfficerActive(int userId, boolean active) {
        users.setActive(userId, active);
        if (active) {
            grievances.assignUnassignedToOfficer(userId);
        }
    }

    public List<AdminGrievanceRow> listGrievances(String status, String priority, String search) {
        return grievances.listAllGrievances(status, priority, search);
    }

    public List<com.jansunwai.dto.GrievanceDtos.HistoryItem> getGrievanceHistory(String trackingId) {
        return grievances.findHistory(trackingId);
    }



    @Transactional
    public void assignGrievance(String trackingId, int officerId, int adminUserId) {
        int grievanceId = grievances.findGrievanceIdByTrackingId(trackingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Grievance not found"));
        users.findById(officerId)
                .filter(u -> u.role().equals("OFFICER"))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Officer not found"));
        grievances.assignByTrackingId(trackingId, officerId);
        grievances.addHistory(grievanceId, "PENDING", "Reassigned by Admin", adminUserId);
    }
}
