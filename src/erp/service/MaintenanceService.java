package erp.service;

import erp.data.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MaintenanceService {
    private static final String SELECT_FLAG = "SELECT value FROM settings WHERE `key`='maintenance'";
    private static final String UPSERT_FLAG = "INSERT INTO settings(`key`, value) VALUES('maintenance', ?) " +
            "ON DUPLICATE KEY UPDATE value = VALUES(value)";

    public boolean isMaintenanceMode() {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_FLAG);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return Boolean.parseBoolean(rs.getString("value"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to read maintenance flag", e);
        }
        return false;
    }

    public void setMaintenanceMode(boolean enabled) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_FLAG)) {
            ps.setString(1, Boolean.toString(enabled));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Unable to update maintenance flag", e);
        }
    }
}

