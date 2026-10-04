package com.jansunwai.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads the city and category lists used by dropdowns. */
@Repository
public class LookupRepository {

    public record Option(int id, String name) {
    }

    private final JdbcTemplate jdbc;

    public LookupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Option> cities() {
        return jdbc.query("SELECT city_id, name FROM city ORDER BY name",
                (rs, i) -> new Option(rs.getInt("city_id"), rs.getString("name")));
    }

    public List<Option> categories() {
        return jdbc.query("SELECT category_id, name FROM category ORDER BY category_id",
                (rs, i) -> new Option(rs.getInt("category_id"), rs.getString("name")));
    }

    public boolean cityExists(int id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM city WHERE city_id = ?)", Boolean.class, id));
    }

    public boolean categoryExists(int id) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM category WHERE category_id = ?)", Boolean.class, id));
    }
}
