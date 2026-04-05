package erp.service;

import erp.data.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class AdminService {

    public void createUser(String username, String password, String role) {
        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO users_auth(username, role, password_hash) VALUES(?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, role);
            ps.setString(3, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create user", ex);
        }
    }

    public void createCourse(String code, String title, int credits) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO courses(code, title, credits) VALUES(?,?,?)")) {
            ps.setString(1, code);
            ps.setString(2, title);
            ps.setInt(3, credits);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create course", ex);
        }
    }

    public void createSection(int courseId, int instructorId, String dayTime, String room,
                              int capacity, String semester, int year) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO sections(course_id, instructor_id, day_time, room, capacity, semester, academic_year) " +
                             "VALUES(?,?,?,?,?,?,?)")) {
            ps.setInt(1, courseId);
            ps.setInt(2, instructorId);
            ps.setString(3, dayTime);
            ps.setString(4, room);
            ps.setInt(5, capacity);
            ps.setString(6, semester);
            ps.setInt(7, year);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create section", ex);
        }
    }

    public void assignInstructor(int sectionId, int instructorId) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE sections SET instructor_id=? WHERE section_id=?")) {
            ps.setInt(1, instructorId);
            ps.setInt(2, sectionId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to assign instructor", ex);
        }
    }

    public void toggleMaintenance(boolean on) {
        new MaintenanceService().setMaintenanceMode(on);
    }

    public void createStudentProfile(int userId, String rollNo, String program, int year) {
        String sql = "INSERT INTO students(user_id, roll_no, program, academic_year) VALUES(?,?,?,?)";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, rollNo);
            ps.setString(3, program);
            ps.setInt(4, year);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create student profile", ex);
        }
    }

    public void createInstructorProfile(int userId, String department, String title) {
        String sql = "INSERT INTO instructors(user_id, department, title) VALUES(?,?,?)";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, department);
            ps.setString(3, title);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to create instructor profile", ex);
        }
    }
}

