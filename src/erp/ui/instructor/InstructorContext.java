package erp.ui.instructor;

import erp.service.InstructorService;
import erp.service.MaintenanceService;

final class InstructorContext {
    final int userId;
    final String username;
    final InstructorService instructorService = new InstructorService();
    final MaintenanceService maintenanceService = new MaintenanceService();

    InstructorDashboard dashboard;

    InstructorContext(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}

