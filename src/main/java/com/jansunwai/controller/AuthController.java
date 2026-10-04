package com.jansunwai.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jansunwai.dto.AuthDtos.LoginRequest;
import com.jansunwai.dto.AuthDtos.LoginResponse;
import com.jansunwai.dto.AuthDtos.RegisterRequest;
import com.jansunwai.dto.AuthDtos.RegisterResponse;
import com.jansunwai.dto.AuthDtos.UserInfo;
import com.jansunwai.model.User;
import com.jansunwai.security.AuthInterceptor;
import com.jansunwai.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** HTTP layer only: receives JSON, calls the service, returns JSON. No SQL here. */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/auth/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout((String) request.getAttribute(AuthInterceptor.TOKEN));
    }

    @GetMapping("/me")
    public UserInfo me(HttpServletRequest request) {
        User user = (User) request.getAttribute(AuthInterceptor.CURRENT_USER);
        return new UserInfo(user.userId(), user.fullName(), user.username(), user.role());
    }
}
