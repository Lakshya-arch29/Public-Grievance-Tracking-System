package com.jansunwai.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jansunwai.dto.AdminDtos.AdminGrievanceRow;
import com.jansunwai.dto.AdminDtos.AssignRequest;
import com.jansunwai.dto.AdminDtos.CreateOfficerRequest;
import com.jansunwai.dto.AdminDtos.DashboardStats;
import com.jansunwai.dto.AdminDtos.OfficerRow;
import com.jansunwai.dto.AuthDtos.RegisterResponse;
import com.jansunwai.model.User;
import com.jansunwai.security.AuthInterceptor;
import com.jansunwai.service.AdminService;

import jakarta.validation.Valid;

/** Admin endpoints. The interceptor already checked the role is ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DashboardStats dashboard() {
        return service.dashboardStats();
    }

    @PostMapping("/officers")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse createOfficer(@Valid @RequestBody CreateOfficerRequest request) {
        return service.createOfficer(request);
    }

    @GetMapping("/officers")
    public List<OfficerRow> officers(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) Boolean active) {
        return service.listOfficers(search, active);
    }

    @PutMapping("/officers/{userId}/active")
    public Map<String, String> toggleActive(@PathVariable int userId, @RequestBody Map<String, Boolean> body) {
        boolean active = body.getOrDefault("active", true);
        service.toggleOfficerActive(userId, active);
        return Map.of("message", active ? "Officer activated" : "Officer deactivated");
    }

    @GetMapping("/grievances")
    public List<AdminGrievanceRow> grievances(@RequestParam(required = false) String status,
                                              @RequestParam(required = false) String priority,
                                              @RequestParam(required = false) String search) {
        return service.listGrievances(status, priority, search);
    }

    @GetMapping("/grievances/{trackingId}/history")
    public List<com.jansunwai.dto.GrievanceDtos.HistoryItem> getHistory(@PathVariable String trackingId) {
        return service.getGrievanceHistory(trackingId);
    }



    @PutMapping("/grievances/{trackingId}/assign")
    public Map<String, String> assign(@PathVariable String trackingId,
                                       @Valid @RequestBody AssignRequest request,
                                       @RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        service.assignGrievance(trackingId, request.officerId(), user.userId());
        return Map.of("message", "Grievance assigned");
    }
}
