package com.jansunwai.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jansunwai.model.User;

/** SQL for the auth_token table (one row per active login). */
@Repository
public class TokenRepository {

    private final JdbcTemplate jdbc;

    public TokenRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(String token, int userId, LocalDateTime expiresAt) {
        jdbc.update("INSERT INTO auth_token (token, user_id, expires_at) VALUES (?, ?, ?)",
                token, userId, Timestamp.valueOf(expiresAt));
    }

    /** Returns the owner of the token, only if the token exists and has not expired. */
    public Optional<User> findUserByToken(String token) {
        return jdbc.query("""
                SELECT u.* FROM auth_token t
                JOIN users u ON u.user_id = t.user_id
                WHERE t.token = ? AND t.expires_at > NOW()
                """, UserRepository.MAPPER, token).stream().findFirst();
    }

    public void delete(String token) {
        jdbc.update("DELETE FROM auth_token WHERE token = ?", token);
    }
}
