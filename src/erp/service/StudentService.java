package erp.service;

import erp.data.DBConnection;
import erp.data.SettingDAO;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class StudentService {

    public List<SectionDisplay> fetchCatalog() {
        List<SectionDisplay> sections = new ArrayList<>();
        final String sql = """
                SELECT s.section_id,
                       c.code,
                       c.title,
                       c.credits,
                       s.day_time,
                       s.room,
                       s.capacity,
                       s.semester,
                       s.academic_year,
                       (SELECT COUNT(*) FROM enrollments e
                         WHERE e.section_id = s.section_id AND e.status='ENROLLED') AS enrolled,
                       ua.username AS instructor
                FROM sections s
                JOIN courses c ON s.course_id = c.course_id
                LEFT JOIN users_auth ua ON s.instructor_id = ua.user_id
                ORDER BY c.code, s.day_time
                """;
        try (Connection conn = DBConnection.getERPConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                sections.add(mapSection(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load catalog", ex);
        }
        return sections;
    }

    public List<SectionDisplay> fetchRegistrations(int studentId) {
        List<SectionDisplay> registrations = new ArrayList<>();
        final String sql = """
                SELECT s.section_id,
                       c.code,
                       c.title,
                       c.credits,
                       s.day_time,
                       s.room,
                       s.capacity,
                       s.semester,
                       s.academic_year,
                       (SELECT COUNT(*) FROM enrollments e2
                         WHERE e2.section_id = s.section_id AND e2.status='ENROLLED') AS enrolled,
                       ua.username AS instructor
                FROM enrollments e
                JOIN sections s ON e.section_id = s.section_id
                JOIN courses c ON s.course_id = c.course_id
                LEFT JOIN users_auth ua ON s.instructor_id = ua.user_id
                WHERE e.student_id=? AND e.status='ENROLLED'
                ORDER BY c.code
                """;
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                registrations.add(mapSection(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load registrations", ex);
        }
        return registrations;
    }

    public List<GradeRow> fetchGrades(int studentId) {
        List<GradeRow> grades = new ArrayList<>();
        final String sql = """
                SELECT c.code,
                       c.title,
                       g.component,
                       g.score,
                       g.final_grade
                FROM enrollments e
                JOIN sections s ON e.section_id = s.section_id
                JOIN courses c ON s.course_id = c.course_id
                LEFT JOIN grades g ON e.enrollment_id = g.enrollment_id
                WHERE e.student_id=?
                ORDER BY c.code, g.component
                """;
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                grades.add(new GradeRow(
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("component"),
                        rs.getDouble("score"),
                        rs.getString("final_grade")
                ));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load grades", ex);
        }
        return grades;
    }

    public List<TimetableRow> fetchTimetable(int studentId) {
        List<TimetableRow> timetable = new ArrayList<>();
        final String sql = """
                SELECT c.code,
                       c.title,
                       s.day_time,
                       s.room,
                       s.semester,
                       s.academic_year
                FROM enrollments e
                JOIN sections s ON e.section_id = s.section_id
                JOIN courses c ON s.course_id = c.course_id
                WHERE e.student_id=? AND e.status='ENROLLED'
                ORDER BY s.day_time
                """;
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                timetable.add(new TimetableRow(
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("day_time"),
                        rs.getString("room"),
                        rs.getString("semester"),
                        rs.getInt("academic_year")
                ));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to load timetable", ex);
        }
        return timetable;
    }

    public void register(int studentId, int sectionId) {
        if (isSectionFull(sectionId)) {
            throw new IllegalStateException("Section full");
        }
        if (hasExistingEnrollment(studentId, sectionId)) {
            throw new IllegalStateException("Already registered in this section");
        }
        enforceRegistrationWindow();

        final String insert = "INSERT INTO enrollments(student_id, section_id, status) VALUES(?,?, 'ENROLLED')";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to register", ex);
        }
    }

    public void drop(int studentId, int sectionId) {
        enforceDropWindow();
        final String sql = "DELETE FROM enrollments WHERE student_id=? AND section_id=? AND status='ENROLLED'";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new IllegalStateException("No active registration found to drop.");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to drop section", ex);
        }
    }

    public Path exportTranscriptCsv(int studentId, Path file) {
        return new erp.util.TranscriptExporter().exportCsv(studentId, file);
    }

    public Path exportTranscriptPdf(int studentId, Path file) {
        return new erp.util.TranscriptExporter().exportPdf(studentId, file);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private SectionDisplay mapSection(ResultSet rs) throws SQLException {
        return new SectionDisplay(
                rs.getInt("section_id"),
                rs.getString("code"),
                rs.getString("title"),
                rs.getInt("credits"),
                rs.getString("day_time"),
                rs.getString("room"),
                rs.getInt("capacity"),
                rs.getInt("enrolled"),
                rs.getString("instructor"),
                rs.getString("semester"),
                rs.getInt("academic_year")
        );
    }

    private boolean hasExistingEnrollment(int studentId, int sectionId) {
        final String sql = "SELECT COUNT(*) FROM enrollments WHERE student_id=? AND section_id=?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, sectionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check enrollment", ex);
        }
        return false;
    }

    private boolean isSectionFull(int sectionId) {
        final String sql = "SELECT COUNT(*) >= capacity AS full FROM enrollments e " +
                "JOIN sections s ON e.section_id = s.section_id WHERE s.section_id=? AND e.status='ENROLLED'";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sectionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("full");
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to check capacity", ex);
        }
        return false;
    }

    private void enforceRegistrationWindow() {
        String openDate = new SettingDAO().findValue("registration_open");
        String closeDate = new SettingDAO().findValue("registration_close");
        LocalDate today = LocalDate.now();
        if (openDate != null && today.isBefore(LocalDate.parse(openDate))) {
            throw new IllegalStateException("Registration window not open yet.");
        }
        if (closeDate != null && today.isAfter(LocalDate.parse(closeDate))) {
            throw new IllegalStateException("Registration window has closed.");
        }
    }

    private void enforceDropWindow() {
        String deadline = new SettingDAO().findValue("drop_deadline");
        if (deadline == null) return;
        LocalDate dropDeadline = LocalDate.parse(deadline);
        if (LocalDate.now().isAfter(dropDeadline)) {
            throw new IllegalStateException("Drop deadline has passed.");
        }
    }

    // ---------------------------------------------------------------------
    // DTOs
    // ---------------------------------------------------------------------
    public record SectionDisplay(
            int sectionId,
            String courseCode,
            String courseTitle,
            int credits,
            String dayTime,
            String room,
            int capacity,
            int enrolled,
            String instructor,
            String semester,
            int year) {
        public int seatsRemaining() {
            return Math.max(0, capacity - enrolled);
        }
    }

    public record GradeRow(
            String courseCode,
            String courseTitle,
            String component,
            double score,
            String finalGrade) {
    }

    public record TimetableRow(
            String courseCode,
            String courseTitle,
            String dayTime,
            String room,
            String semester,
            int year) {
    }
}

