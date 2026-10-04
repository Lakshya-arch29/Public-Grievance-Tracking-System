package com.jansunwai.repository;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.jansunwai.model.User;

/** SQL for the users table. No business rules here. */
@Repository
public class UserRepository {

    public static final RowMapper<User> MAPPER = (rs, rowNum) -> new User(
            rs.getInt("user_id"),
            rs.getString("full_name"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("password_hash"),
            rs.getString("role"),
            rs.getBoolean("active"));

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByUsername(String username) {
        return jdbc.query("SELECT * FROM users WHERE username = ?", MAPPER, username)
                .stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE username = ?)", Boolean.class, username));
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = ?)", Boolean.class, email));
    }

    /** Sign-up always creates a CITIZEN. Returns the new user id. */
    public int insertCitizen(String fullName, String username, String email, String phone, String passwordHash) {
        Integer id = jdbc.queryForObject("""
                INSERT INTO users (full_name, username, email, phone, password_hash, role)
                VALUES (?, ?, ?, ?, ?, 'CITIZEN')
                RETURNING user_id
                """, Integer.class, fullName, username, email, phone, passwordHash);
        return id;
    }
}
