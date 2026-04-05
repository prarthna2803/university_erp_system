package erp.data;

import erp.domain.Student;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentDAO {

    private static final String SELECT_BY_USER =
            "SELECT user_id, roll_no, program, academic_year FROM students WHERE user_id = ?";

    public Student findByUserId(int userId) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Student(
                        rs.getInt("user_id"),
                        rs.getString("roll_no"),
                        rs.getString("program"),
                        rs.getInt("academic_year")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

