package com.jansunwai.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.jansunwai.dto.AdminDtos.AdminGrievanceRow;
import com.jansunwai.dto.AdminDtos.DashboardStats;
import com.jansunwai.dto.GrievanceDtos.GrievanceDetail;
import com.jansunwai.dto.GrievanceDtos.GrievanceRequest;
import com.jansunwai.dto.GrievanceDtos.GrievanceRow;
import com.jansunwai.dto.GrievanceDtos.HistoryItem;
import com.jansunwai.dto.GrievanceDtos.OfficerInfo;
import com.jansunwai.dto.GrievanceDtos.StatusSummary;
import com.jansunwai.dto.OfficerDtos.OfficerDashboardStats;
import com.jansunwai.dto.OfficerDtos.OfficerGrievanceRow;

/** SQL for grievances and their history. No business rules here. */
@Repository
public class GrievanceRepository {

    /** The id and the tracking id (GRV-00042) the database generated. */
    public record Created(int grievanceId, String trackingId) {
    }

    private final JdbcTemplate jdbc;

    public GrievanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String time(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime().toString();
    }

    // ---------- Create ----------

    public Created insert(int userId, GrievanceRequest r, String priority) {
        return jdbc.queryForObject("""
                INSERT INTO grievance (user_id, category_id, city_id, area, title, description, priority)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING grievance_id, tracking_id
                """,
                (rs, i) -> new Created(rs.getInt("grievance_id"), rs.getString("tracking_id")),
                userId, r.categoryId(), r.cityId(), r.area().trim(), r.title().trim(),
                r.description().trim(), priority);
    }

    public void addHistory(int grievanceId, String status, String remark, Integer changedBy) {
        jdbc.update("INSERT INTO grievance_history (grievance_id, status, remark, changed_by) VALUES (?, ?, ?, ?)",
                grievanceId, status, remark, changedBy);
    }

    public void assign(int grievanceId, int officerId) {
        jdbc.update("UPDATE grievance SET assigned_officer_id = ? WHERE grievance_id = ?", officerId, grievanceId);
    }

    public void assignUnassignedInCity(int cityId, int officerId) {
        jdbc.update("UPDATE grievance SET assigned_officer_id = ?, updated_at = NOW() WHERE assigned_officer_id IS NULL AND status = 'PENDING' AND city_id = ?", officerId, cityId);
    }

    public void assignUnassignedToOfficer(int officerId) {
        jdbc.update("""
            UPDATE grievance 
            SET assigned_officer_id = ?, updated_at = NOW() 
            WHERE assigned_officer_id IS NULL AND status = 'PENDING' 
            AND city_id = (SELECT city_id FROM users WHERE user_id = ?)
            """, officerId, officerId);
    }

    // ---------- Auto-assignment: the active officer with the fewest open grievances ----------

    private static final RowMapper<OfficerInfo> OFFICER_MAPPER = (rs, i) ->
            new OfficerInfo(rs.getInt("user_id"), rs.getString("full_name"), rs.getString("phone"));

    /** Step 1: same city and same category. */
    public Optional<OfficerInfo> findLeastLoadedOfficer(int cityId, int categoryId) {
        return jdbc.query("""
                SELECT u.user_id, u.full_name, u.phone
                FROM users u
                LEFT JOIN grievance g ON g.assigned_officer_id = u.user_id
                                     AND g.status IN ('PENDING', 'IN_PROGRESS')
                WHERE u.role = 'OFFICER' AND u.active
                  AND u.city_id = ? AND u.category_id = ?
                GROUP BY u.user_id, u.full_name, u.phone
                ORDER BY COUNT(g.grievance_id), u.user_id
                LIMIT 1
                """, OFFICER_MAPPER, cityId, categoryId).stream().findFirst();
    }

    /** Step 2: same city, any category. */
    public Optional<OfficerInfo> findLeastLoadedOfficerInCity(int cityId) {
        return jdbc.query("""
                SELECT u.user_id, u.full_name, u.phone
                FROM users u
                LEFT JOIN grievance g ON g.assigned_officer_id = u.user_id
                                     AND g.status IN ('PENDING', 'IN_PROGRESS')
                WHERE u.role = 'OFFICER' AND u.active
                  AND u.city_id = ?
                GROUP BY u.user_id, u.full_name, u.phone
                ORDER BY COUNT(g.grievance_id), u.user_id
                LIMIT 1
                """, OFFICER_MAPPER, cityId).stream().findFirst();
    }

    // ---------- Citizen reads ----------

    public StatusSummary summaryForCitizen(int userId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending,
                       COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS in_progress,
                       COUNT(*) FILTER (WHERE status = 'RESOLVED')    AS resolved,
                       COUNT(*) FILTER (WHERE status = 'REJECTED')    AS rejected
                FROM grievance WHERE user_id = ?
                """,
                (rs, i) -> new StatusSummary(rs.getInt("total"), rs.getInt("pending"),
                        rs.getInt("in_progress"), rs.getInt("resolved"), rs.getInt("rejected")),
                userId);
    }

    /** The citizen's own grievances, newest first. status may be null (= all). */
    public List<GrievanceRow> listForCitizen(int userId, String status) {
        String sql = """
                SELECT g.tracking_id, g.title, cat.name AS category, c.name AS city,
                       g.area, g.priority, g.status, g.created_at
                FROM grievance g
                JOIN category cat ON cat.category_id = g.category_id
                JOIN city c       ON c.city_id = g.city_id
                WHERE g.user_id = ?
                """;
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (status != null) {
            sql += " AND g.status = ?";
            args.add(status);
        }
        sql += " ORDER BY g.created_at DESC, g.grievance_id DESC";

        return jdbc.query(sql, (rs, i) -> new GrievanceRow(
                rs.getString("tracking_id"), rs.getString("title"), rs.getString("category"),
                rs.getString("city"), rs.getString("area"), rs.getString("priority"),
                rs.getString("status"), time(rs, "created_at")), args.toArray());
    }

    /** One grievance with its timeline. Empty if it does not exist or belongs to someone else. */
    public Optional<GrievanceDetail> findForCitizen(int userId, String trackingId) {
        return jdbc.query("""
                SELECT g.tracking_id, g.title, g.description, cat.name AS category, c.name AS city,
                       g.area, g.priority, g.status, g.created_at, g.updated_at,
                       o.user_id AS officer_id, o.full_name AS officer_name, o.phone AS officer_phone
                FROM grievance g
                JOIN category cat ON cat.category_id = g.category_id
                JOIN city c       ON c.city_id = g.city_id
                LEFT JOIN users o ON o.user_id = g.assigned_officer_id
                WHERE g.tracking_id = ? AND g.user_id = ?
                """, (rs, i) -> {
                    Integer officerId = rs.getObject("officer_id", Integer.class);
                    OfficerInfo officer = officerId == null ? null
                            : new OfficerInfo(officerId, rs.getString("officer_name"), rs.getString("officer_phone"));
                    return new GrievanceDetail(rs.getString("tracking_id"), rs.getString("title"),
                            rs.getString("description"), rs.getString("category"), rs.getString("city"),
                            rs.getString("area"), rs.getString("priority"), rs.getString("status"),
                            time(rs, "created_at"), time(rs, "updated_at"), officer, List.of());
                }, trackingId, userId)
                .stream().findFirst()
                .map(detail -> detail.withHistory(findHistory(trackingId)));
    }

    public List<HistoryItem> findHistory(String trackingId) {
        return jdbc.query("""
                SELECT h.status, h.remark, COALESCE(u.full_name, 'System') AS changed_by, h.changed_at
                FROM grievance_history h
                JOIN grievance g ON g.grievance_id = h.grievance_id
                LEFT JOIN users u ON u.user_id = h.changed_by
                WHERE g.tracking_id = ?
                ORDER BY h.changed_at, h.history_id
                """, (rs, i) -> new HistoryItem(rs.getString("status"), rs.getString("remark"),
                rs.getString("changed_by"), time(rs, "changed_at")), trackingId);
    }

    // ---------- Admin reads ----------

    public DashboardStats adminDashboardStats() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending,
                       COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS in_progress,
                       COUNT(*) FILTER (WHERE status = 'RESOLVED')    AS resolved,
                       COUNT(*) FILTER (WHERE status = 'REJECTED')    AS rejected,
                       COUNT(*) FILTER (WHERE status IN ('PENDING', 'IN_PROGRESS')
                                             AND updated_at < NOW() - INTERVAL '7 days') AS overdue,
                       COUNT(*) FILTER (WHERE assigned_officer_id IS NULL
                                             AND status = 'PENDING') AS unassigned
                FROM grievance
                """,
                (rs, i) -> new DashboardStats(rs.getInt("total"), rs.getInt("pending"),
                        rs.getInt("in_progress"), rs.getInt("resolved"), rs.getInt("rejected"),
                        rs.getInt("overdue"), rs.getInt("unassigned")));
    }

    public List<AdminGrievanceRow> listAllGrievances(String status, String priority, String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT g.tracking_id, g.title, cat.name AS category, c.name AS city,
                       g.area, g.priority, g.status,
                       COALESCE(o.full_name, 'Unassigned') AS officer_name,
                       o.phone AS officer_phone, o.email AS officer_email,
                       u.full_name AS citizen_name, g.created_at
                FROM grievance g
                JOIN category cat ON cat.category_id = g.category_id
                JOIN city c       ON c.city_id = g.city_id
                JOIN users u      ON u.user_id = g.user_id
                LEFT JOIN users o ON o.user_id = g.assigned_officer_id
                WHERE 1=1
                """);

        List<Object> args = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND g.status = ?");
            args.add(status);
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND g.priority = ?");
            args.add(priority);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(g.title) LIKE ? OR g.tracking_id LIKE ? OR LOWER(g.area) LIKE ?)");
            String pattern = "%" + search.toLowerCase() + "%";
            args.add(pattern);
            args.add("%" + search.toUpperCase() + "%");
            args.add(pattern);
        }

        sql.append(" ORDER BY g.created_at DESC, g.grievance_id DESC");

        return jdbc.query(sql.toString(), (rs, i) -> new AdminGrievanceRow(
                rs.getString("tracking_id"), rs.getString("title"), rs.getString("category"),
                rs.getString("city"), rs.getString("area"), rs.getString("priority"),
                rs.getString("status"), rs.getString("officer_name"), 
                rs.getString("officer_phone"), rs.getString("officer_email"),
                rs.getString("citizen_name"),
                time(rs, "created_at")), args.toArray());
    }

    /** Update status of any grievance (by admin or officer). */
    public void updateStatus(String trackingId, String status) {
        jdbc.update("""
                UPDATE grievance SET status = ?, updated_at = NOW(),
                       resolved_at = CASE WHEN ? = 'RESOLVED' THEN NOW() ELSE resolved_at END
                WHERE tracking_id = ?
                """, status, status, trackingId);
    }

    /** Assign a grievance to a specific officer. */
    public void assignByTrackingId(String trackingId, int officerId) {
        jdbc.update("UPDATE grievance SET assigned_officer_id = ?, updated_at = NOW() WHERE tracking_id = ?",
                officerId, trackingId);
    }

    /** Get the internal grievance_id from tracking_id. */
    public Optional<Integer> findGrievanceIdByTrackingId(String trackingId) {
        return jdbc.query("SELECT grievance_id FROM grievance WHERE tracking_id = ?",
                (rs, i) -> rs.getInt("grievance_id"), trackingId).stream().findFirst();
    }

    // ---------- Officer reads ----------

    public OfficerDashboardStats officerDashboardStats(int officerId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS assigned,
                       COUNT(*) FILTER (WHERE status = 'PENDING')     AS pending,
                       COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS in_progress,
                       COUNT(*) FILTER (WHERE status = 'RESOLVED')    AS resolved,
                       COUNT(*) FILTER (WHERE status = 'REJECTED')    AS rejected
                FROM grievance WHERE assigned_officer_id = ?
                """,
                (rs, i) -> new OfficerDashboardStats(rs.getInt("assigned"), rs.getInt("pending"),
                        rs.getInt("in_progress"), rs.getInt("resolved"), rs.getInt("rejected")),
                officerId);
    }

    public List<OfficerGrievanceRow> listForOfficer(int officerId, String status, String priority, String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT g.tracking_id, g.title, cat.name AS category, c.name AS city,
                       g.area, g.priority, g.status, u.full_name AS citizen_name, g.created_at
                FROM grievance g
                JOIN category cat ON cat.category_id = g.category_id
                JOIN city c       ON c.city_id = g.city_id
                JOIN users u      ON u.user_id = g.user_id
                WHERE g.assigned_officer_id = ?
                """);

        List<Object> args = new ArrayList<>();
        args.add(officerId);

        if (status != null && !status.isBlank()) {
            sql.append(" AND g.status = ?");
            args.add(status);
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND g.priority = ?");
            args.add(priority);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(g.title) LIKE ? OR LOWER(g.area) LIKE ?)");
            String pattern = "%" + search.toLowerCase() + "%";
            args.add(pattern);
            args.add(pattern);
        }

        sql.append(" ORDER BY g.created_at DESC, g.grievance_id DESC");

        return jdbc.query(sql.toString(), (rs, i) -> new OfficerGrievanceRow(
                rs.getString("tracking_id"), rs.getString("title"), rs.getString("category"),
                rs.getString("city"), rs.getString("area"), rs.getString("priority"),
                rs.getString("status"), rs.getString("citizen_name"),
                time(rs, "created_at")), args.toArray());
    }

    /** Grievance detail for officer — only returns if assigned to this officer. */
    public Optional<GrievanceDetail> findForOfficer(int officerId, String trackingId) {
        return jdbc.query("""
                SELECT g.tracking_id, g.title, g.description, cat.name AS category, c.name AS city,
                       g.area, g.priority, g.status, g.created_at, g.updated_at,
                       o.user_id AS officer_id, o.full_name AS officer_name, o.phone AS officer_phone
                FROM grievance g
                JOIN category cat ON cat.category_id = g.category_id
                JOIN city c       ON c.city_id = g.city_id
                LEFT JOIN users o ON o.user_id = g.assigned_officer_id
                WHERE g.tracking_id = ? AND g.assigned_officer_id = ?
                """, (rs, i) -> {
                    Integer oid = rs.getObject("officer_id", Integer.class);
                    OfficerInfo officer = oid == null ? null
                            : new OfficerInfo(oid, rs.getString("officer_name"), rs.getString("officer_phone"));
                    return new GrievanceDetail(rs.getString("tracking_id"), rs.getString("title"),
                            rs.getString("description"), rs.getString("category"), rs.getString("city"),
                            rs.getString("area"), rs.getString("priority"), rs.getString("status"),
                            time(rs, "created_at"), time(rs, "updated_at"), officer, List.of());
                }, trackingId, officerId)
                .stream().findFirst()
                .map(detail -> detail.withHistory(findHistory(trackingId)));
    }
}

