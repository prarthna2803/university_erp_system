package erp.service;

public class AccessControlService {

    private final MaintenanceService maintenanceService = new MaintenanceService();

    public void ensureWritable(String role) {
        if (maintenanceService.isMaintenanceMode()
                && (role.equalsIgnoreCase("STUDENT") || role.equalsIgnoreCase("INSTRUCTOR"))) {
            throw new IllegalStateException("Maintenance mode is ON. Changes are blocked.");
        }
    }

    public void ensureRole(String actualRole, String... allowedRoles) {
        for (String allowed : allowedRoles) {
            if (allowed.equalsIgnoreCase(actualRole)) {
                return;
            }
        }
        throw new SecurityException("You are not allowed to perform this action.");
    }
}

