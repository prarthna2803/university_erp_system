package erp.service;

import erp.data.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AuthService {

    // authenticate user and return user_id + role
    public AuthResult authenticate(String username, String password) {

        String sql = "SELECT user_id, role, password_hash FROM users_auth WHERE username=?";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");
                String hash = rs.getString("password_hash");

                if (BCrypt.checkpw(password, hash)) {
                    return new AuthResult(userId, role);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
