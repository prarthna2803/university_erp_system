package erp.ui.admin;

import erp.service.AdminService;
import erp.service.MaintenanceService;

final class AdminContext {
    final int userId;
    final String username;
    final AdminService adminService = new AdminService();
    final MaintenanceService maintenanceService = new MaintenanceService();

    AdminContext(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}

