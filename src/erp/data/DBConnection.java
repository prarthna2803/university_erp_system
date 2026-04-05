package erp.data;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String AUTH_URL = "jdbc:mysql://localhost:3306/authdb";
    private static final String ERP_URL = "jdbc:mysql://localhost:3306/erpdb";
    private static final String USER = "root";
    private static final String PASS = "mysql@123";

    public static Connection getAuthConnection() {
        try {
            return DriverManager.getConnection(AUTH_URL, USER, PASS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Connection getERPConnection() {
        try {
            return DriverManager.getConnection(ERP_URL, USER, PASS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        try (Connection c1 = getAuthConnection();
             Connection c2 = getERPConnection()) {

            System.out.println("Auth DB OK: " + (c1 != null));
            System.out.println("ERP DB OK: " + (c2 != null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
