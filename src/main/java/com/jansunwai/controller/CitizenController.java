package com.jansunwai.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jansunwai.dto.GrievanceDtos.CreateResponse;
import com.jansunwai.dto.GrievanceDtos.GrievanceDetail;
import com.jansunwai.dto.GrievanceDtos.GrievanceRequest;
import com.jansunwai.dto.GrievanceDtos.GrievanceRow;
import com.jansunwai.dto.GrievanceDtos.StatusSummary;
import com.jansunwai.model.User;
import com.jansunwai.security.AuthInterceptor;
import com.jansunwai.service.GrievanceService;

import jakarta.validation.Valid;

/** Citizen endpoints. The interceptor already checked the role is CITIZEN. */
@RestController
@RequestMapping("/api/citizen")
public class CitizenController {

    private final GrievanceService service;

    public CitizenController(GrievanceService service) {
        this.service = service;
    }

    @PostMapping("/grievances")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResponse file(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
                               @Valid @RequestBody GrievanceRequest request) {
        return service.file(user.userId(), request);
    }

    @GetMapping("/summary")
    public StatusSummary summary(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user) {
        return service.summary(user.userId());
    }

    @GetMapping("/grievances")
    public List<GrievanceRow> list(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
                                   @RequestParam(required = false) String status) {
        return service.list(user.userId(), status);
    }

    @GetMapping("/grievances/{trackingId}")
    public GrievanceDetail detail(@RequestAttribute(AuthInterceptor.CURRENT_USER) User user,
                                  @PathVariable String trackingId) {
        return service.detail(user.userId(), trackingId);
    }
}
