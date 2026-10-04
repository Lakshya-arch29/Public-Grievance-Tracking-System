package com.jansunwai.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the demo accounts when the app starts.
 * Passwords are hashed here with the same BCrypt encoder that login uses,
 * so the demo passwords always match. ON CONFLICT DO NOTHING makes it safe to
 * restart the app any number of times.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        // Admin
        addUser("Admin User", "admin", "admin@example.com", "9800000000",
                "Admin@123", "ADMIN", null, null);

        // Clean up the old default officers so they don't clutter your dashboard
        // First remove their associations from the grievance table to prevent foreign key errors
        jdbc.update("UPDATE grievance SET assigned_officer_id = NULL WHERE assigned_officer_id IN (SELECT user_id FROM users WHERE username IN ('sharma', 'patil', 'kulkarni', 'deshmukh', 'iyer', 'khan'))");
        jdbc.update("UPDATE grievance_history SET changed_by = NULL WHERE changed_by IN (SELECT user_id FROM users WHERE username IN ('sharma', 'patil', 'kulkarni', 'deshmukh', 'iyer', 'khan'))");
        jdbc.update("DELETE FROM users WHERE username IN ('sharma', 'patil', 'kulkarni', 'deshmukh', 'iyer', 'khan')");

        // Citizens
        addUser("Rahul Verma", "rahul", "rahul@example.com", "9876543210", "Citizen@123", "CITIZEN", null, null);
        addUser("Priya Singh", "priya", "priya@example.com", "9876543211", "Citizen@123", "CITIZEN", null, null);
        addUser("Amit Joshi",  "amit",  "amit@example.com",  "9876543212", "Citizen@123", "CITIZEN", null, null);
        addUser("Sneha Rao",   "sneha", "sneha@example.com", "9876543213", "Citizen@123", "CITIZEN", null, null);
    }

    private void addUser(String fullName, String username, String email, String phone,
                         String password, String role, String cityName, String categoryName) {
        jdbc.update("""
                INSERT INTO users (full_name, username, email, phone, password_hash, role, city_id, category_id)
                VALUES (?, ?, ?, ?, ?, ?,
                        (SELECT city_id FROM city WHERE name = ?),
                        (SELECT category_id FROM category WHERE name = ?))
                ON CONFLICT (username) DO NOTHING
                """,
                fullName, username, email, phone, encoder.encode(password), role, cityName, categoryName);
    }
}
