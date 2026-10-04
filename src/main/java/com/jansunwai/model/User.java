package com.jansunwai.model;

/** One row of the users table. Used inside the server only, never sent to the browser. */
public record User(int userId, String fullName, String username, String email,
                   String phone, String passwordHash, String role, boolean active) {
}
