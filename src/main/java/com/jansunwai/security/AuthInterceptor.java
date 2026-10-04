package com.jansunwai.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.jansunwai.exception.ApiException;
import com.jansunwai.model.User;
import com.jansunwai.repository.TokenRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before every protected /api request.
 * 1. Reads the "Authorization: Bearer ..." header and checks the token in the database.
 * 2. Checks that the user's role is allowed for the URL (/api/citizen, /api/officer, /api/admin).
 * 3. Puts the logged-in user on the request so controllers can read it.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";
    public static final String TOKEN = "token";

    private final TokenRepository tokens;

    public AuthInterceptor(TokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in");
        }
        String token = header.substring(7).trim();

        User user = tokens.findUserByToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session expired, please log in again"));
        if (!user.active()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        String path = request.getRequestURI();
        requireRole(path, "/api/citizen/", "CITIZEN", user);
        requireRole(path, "/api/officer/", "OFFICER", user);
        requireRole(path, "/api/admin/", "ADMIN", user);

        request.setAttribute(CURRENT_USER, user);
        request.setAttribute(TOKEN, token);
        return true;
    }

    private void requireRole(String path, String prefix, String role, User user) {
        if (path.startsWith(prefix) && !user.role().equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to use this page");
        }
    }
}
