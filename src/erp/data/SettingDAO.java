package erp.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SettingDAO {

    private static final String SELECT_VALUE = "SELECT value FROM settings WHERE `key`=?";

    public String findValue(String key) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_VALUE)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

