package com.jansunwai.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.jansunwai.dto.AdminDtos.OfficerRow;
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

    public Optional<User> findById(int userId) {
        return jdbc.query("SELECT * FROM users WHERE user_id = ?", MAPPER, userId)
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

    /** Admin creates an OFFICER with a city and category assignment. */
    public int insertOfficer(String fullName, String username, String email, String phone,
                             String passwordHash, int cityId) {
        Integer id = jdbc.queryForObject("""
                INSERT INTO users (full_name, username, email, phone, password_hash, role, city_id)
                VALUES (?, ?, ?, ?, ?, 'OFFICER', ?)
                RETURNING user_id
                """, Integer.class, fullName, username, email, phone, passwordHash, cityId);
        return id;
    }

    /** List all officers with their city, category, and grievance counts. */
    public List<OfficerRow> listOfficers(String search, Boolean active) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.user_id, u.full_name, u.username, u.email,
                       COALESCE(c.name, '-') AS city,
                       u.active,
                       COUNT(g.grievance_id) FILTER (WHERE g.status IN ('PENDING', 'IN_PROGRESS')) AS pending,
                       COUNT(g.grievance_id) FILTER (WHERE g.status = 'RESOLVED') AS resolved
                FROM users u
                LEFT JOIN city c ON c.city_id = u.city_id
                LEFT JOIN grievance g ON g.assigned_officer_id = u.user_id
                WHERE u.role = 'OFFICER'
                """);

        java.util.List<Object> args = new java.util.ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(u.full_name) LIKE ? OR LOWER(u.username) LIKE ?)");
            String pattern = "%" + search.toLowerCase() + "%";
            args.add(pattern);
            args.add(pattern);
        }
        if (active != null) {
            sql.append(" AND u.active = ?");
            args.add(active);
        }

        sql.append(" GROUP BY u.user_id, u.full_name, u.username, u.email, c.name, u.active");
        sql.append(" ORDER BY u.full_name");

        return jdbc.query(sql.toString(), (rs, i) -> new OfficerRow(
                rs.getInt("user_id"), rs.getString("full_name"), rs.getString("username"),
                rs.getString("email"), rs.getString("city"),
                rs.getBoolean("active"), rs.getInt("pending"), rs.getInt("resolved")),
                args.toArray());
    }

    /** Toggle an officer's active status. */
    public void setActive(int userId, boolean active) {
        jdbc.update("UPDATE users SET active = ? WHERE user_id = ? AND role = 'OFFICER'", active, userId);
    }
}
