package com.jansunwai.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jansunwai.dto.AuthDtos.LoginRequest;
import com.jansunwai.dto.AuthDtos.LoginResponse;
import com.jansunwai.dto.AuthDtos.RegisterRequest;
import com.jansunwai.dto.AuthDtos.RegisterResponse;
import com.jansunwai.dto.AuthDtos.UserInfo;
import com.jansunwai.exception.ApiException;
import com.jansunwai.model.User;
import com.jansunwai.repository.TokenRepository;
import com.jansunwai.repository.UserRepository;

/** Business rules for sign-up, login and logout. */
@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_HOURS = 8;

    private final UserRepository users;
    private final TokenRepository tokens;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, TokenRepository tokens, PasswordEncoder encoder) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    public RegisterResponse register(RegisterRequest req) {
        String username = req.username().trim().toLowerCase();
        String email = req.email().trim().toLowerCase();

        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        int id = users.insertCitizen(req.fullName().trim(), username, email,
                req.phone(), encoder.encode(req.password()));
        return new RegisterResponse(id, username, "CITIZEN");
    }

    public LoginResponse login(LoginRequest req) {
        User user = users.findByUsername(req.username().trim().toLowerCase()).orElse(null);

        // Same message for "no such user" and "wrong password", so usernames cannot be guessed
        if (user == null || !encoder.matches(req.password(), user.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        String wanted = req.role().trim().toUpperCase();
        if (!user.role().equals(wanted)) {
            String name = wanted.charAt(0) + wanted.substring(1).toLowerCase();
            String article = wanted.equals("ADMIN") ? "an" : "a";
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is not " + article + " " + name + " account");
        }
        if (!user.active()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);   // 64 hex characters
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_HOURS);
        tokens.save(token, user.userId(), expiresAt);

        return new LoginResponse(token, expiresAt.toString(),
                new UserInfo(user.userId(), user.fullName(), user.username(), user.role()));
    }

    public void logout(String token) {
        tokens.delete(token);
    }
}
