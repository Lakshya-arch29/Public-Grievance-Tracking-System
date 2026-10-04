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

        // Officers: name, username, email, phone, city, category
        addUser("Mr. Sharma",    "sharma",    "sharma@example.com",    "9800000001", "Officer@123", "OFFICER", "Pune",      "Roads & Potholes");
        addUser("Ms. Patil",     "patil",     "patil@example.com",     "9800000003", "Officer@123", "OFFICER", "Thane",     "Water Supply");
        addUser("Mr. Kulkarni",  "kulkarni",  "kulkarni@example.com",  "9800000004", "Officer@123", "OFFICER", "Mumbai",    "Garbage & Cleanliness");
        addUser("Mr. Deshmukh",  "deshmukh",  "deshmukh@example.com",  "9800000005", "Officer@123", "OFFICER", "Nagpur",    "Electricity & Street Lights");
        addUser("Ms. Iyer",      "iyer",      "iyer@example.com",      "9800000006", "Officer@123", "OFFICER", "Bengaluru", "Drainage & Sewage");
        addUser("Mr. Khan",      "khan",      "khan@example.com",      "9800000007", "Officer@123", "OFFICER", "Delhi",     "Pollution");

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
