package com.jansunwai.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jansunwai.dto.GrievanceDtos.GrievanceDetail;
import com.jansunwai.dto.OfficerDtos.OfficerDashboardStats;
import com.jansunwai.dto.OfficerDtos.OfficerGrievanceRow;
import com.jansunwai.dto.OfficerDtos.OfficerUpdateRequest;
import com.jansunwai.model.User;
import com.jansunwai.security.AuthInterceptor;
import com.jansunwai.service.OfficerService;

import jakarta.validation.Valid;

/** Officer endpoints. The interceptor already checked the role is OFFICER. */
@RestController
@RequestMapping("/api/officer")
public class OfficerController {

    private final OfficerService service;

    public OfficerController(OfficerService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public OfficerDashboardStats dashboard(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return service.dashboardStats(user.userId());
    }

    @GetMapping("/grievances")
    public List<OfficerGrievanceRow> grievances(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String priority,
                                                 @RequestParam(required = false) String search) {
        return service.listGrievances(user.userId(), status, priority, search);
    }

    @GetMapping("/grievances/{trackingId}")
    public GrievanceDetail detail(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
                                   @PathVariable String trackingId) {
        return service.grievanceDetail(user.userId(), trackingId);
    }

    @PutMapping("/grievances/{trackingId}/status")
    public Map<String, String> updateStatus(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
                                             @PathVariable String trackingId,
                                             @Valid @RequestBody OfficerUpdateRequest request) {
        service.updateGrievanceStatus(user.userId(), trackingId, request);
        return Map.of("message", "Status updated to " + request.status());
    }
}
