package erp.data;

import erp.domain.Instructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InstructorDAO {

    private static final String SELECT_BY_USER =
            "SELECT user_id, department, title FROM instructors WHERE user_id = ?";

    public Instructor findByUserId(int userId) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Instructor(
                        rs.getInt("user_id"),
                        rs.getString("department"),
                        rs.getString("title")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

