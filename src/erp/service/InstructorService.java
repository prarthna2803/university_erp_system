package erp.service;

import erp.data.DBConnection;

import java.sql.*;
import java.util.*;

public class InstructorService {

    public List<SectionRow> listMySections(int instructorId) {
        List<SectionRow> rows = new ArrayList<>();
        String sql = """
            SELECT 
                s.section_id,
                c.code AS course_code,
                c.title AS course_title,
                CONCAT(s.day, ' ', s.time) AS day_time,
                s.room
            FROM sections s
            JOIN courses c ON s.course_id = c.course_id
            WHERE s.instructor_id = ?
            ORDER BY c.code
        """;
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new SectionRow(
                        rs.getInt("section_id"),
                        rs.getString("course_code"),
                        rs.getString("course_title"),
                        rs.getString("day_time"),
                        rs.getString("room")
                ));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to load instructor sections", ex);
        }
        return rows;
    }

    public List<StudentGradeRow> loadEnrollmentGrades(int sectionId) {
        List<StudentGradeRow> rows = new ArrayList<>();
        String sql = """
                SELECT e.enrollment_id,
                       ua.username AS student,
                       COALESCE(MAX(CASE WHEN component='QUIZ' THEN score END),0) AS quiz,
                       COALESCE(MAX(CASE WHEN component='MIDTERM' THEN score END),0) AS midterm,
                       COALESCE(MAX(CASE WHEN component='ENDSEM' THEN score END),0) AS endsem,
                       COALESCE(MAX(final_grade), '') AS finalGrade
                FROM enrollments e
                JOIN users_auth ua ON e.student_id = ua.user_id
                LEFT JOIN grades g ON e.enrollment_id = g.enrollment_id
                WHERE e.section_id=?
                GROUP BY e.enrollment_id, ua.username
                ORDER BY ua.username
                """;
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new StudentGradeRow(
                        rs.getInt("enrollment_id"),
                        rs.getString("student"),
                        rs.getDouble("quiz"),
                        rs.getDouble("midterm"),
                        rs.getDouble("endsem"),
                        rs.getString("finalGrade")
                ));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to load enrollment grades", ex);
        }
        return rows;
    }

    public Map<String, Double> computeClassStats(int sectionId) {
        Map<String, Double> stats = new LinkedHashMap<>();
        String sql = "SELECT AVG(score) AS avgScore, MIN(score) AS minScore, MAX(score) AS maxScore " +
                "FROM grades WHERE section_id=?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                stats.put("average", rs.getDouble("avgScore"));
                stats.put("min", rs.getDouble("minScore"));
                stats.put("max", rs.getDouble("maxScore"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to compute stats", ex);
        }
        return stats;
    }

    public void upsertComponentScore(int instructorId, int sectionId, int enrollmentId, String component, double score) {
        ensureSectionOwnership(instructorId, sectionId);
        String sql = "INSERT INTO grades(enrollment_id, section_id, component, score) " +
                "VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE score=VALUES(score)";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enrollmentId);
            ps.setInt(2, sectionId);
            ps.setString(3, component);
            ps.setDouble(4, score);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to save score", ex);
        }
    }

    public void finalizeGrades(int instructorId, int sectionId, double quizWeight, double midWeight, double endWeight) {
        ensureSectionOwnership(instructorId, sectionId);
        final String finalizeSql = "UPDATE grades g " +
                "JOIN (" +
                "   SELECT e.enrollment_id, " +
                "   COALESCE(MAX(CASE WHEN component='QUIZ' THEN score END),0) AS quiz," +
                "   COALESCE(MAX(CASE WHEN component='MIDTERM' THEN score END),0) AS mid," +
                "   COALESCE(MAX(CASE WHEN component='ENDSEM' THEN score END),0) AS endsem" +
                "   FROM enrollments e LEFT JOIN grades g2 ON e.enrollment_id=g2.enrollment_id" +
                "   WHERE e.section_id=? GROUP BY e.enrollment_id" +
                ") scores ON g.enrollment_id = scores.enrollment_id " +
                "SET g.final_grade = CASE " +
                " WHEN (scores.quiz*? + scores.mid*? + scores.endsem*?) >= 90 THEN 'A'" +
                " WHEN (scores.quiz*? + scores.mid*? + scores.endsem*?) >= 80 THEN 'B'" +
                " WHEN (scores.quiz*? + scores.mid*? + scores.endsem*?) >= 70 THEN 'C'" +
                " ELSE 'D' END " +
                "WHERE g.section_id=?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(finalizeSql)) {
            ps.setInt(1, sectionId);
            ps.setDouble(2, quizWeight);
            ps.setDouble(3, midWeight);
            ps.setDouble(4, endWeight);
            ps.setDouble(5, quizWeight);
            ps.setDouble(6, midWeight);
            ps.setDouble(7, endWeight);
            ps.setDouble(8, quizWeight);
            ps.setDouble(9, midWeight);
            ps.setDouble(10, endWeight);
            ps.setInt(11, sectionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to finalize grades", ex);
        }
    }

    private void ensureSectionOwnership(int instructorId, int sectionId) {
        String sql = "SELECT COUNT(*) FROM sections WHERE section_id=? AND instructor_id=?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ps.setInt(2, instructorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                throw new SecurityException("Not your section.");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to verify section ownership", ex);
        }
    }

    public record SectionRow(int sectionId, String courseCode, String courseTitle, String dayTime, String room) {}

    public record StudentGradeRow(int enrollmentId, String studentName, double quiz, double mid, double end, String finalGrade) {}
}

