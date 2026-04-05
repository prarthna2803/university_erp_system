package erp.ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class AdminMaintenancePanel extends JPanel {

    private final AdminContext context;
    private final JLabel statusLabel = new JLabel();
    private final JToggleButton toggle = new JToggleButton();

    AdminMaintenancePanel(AdminContext context) {
        this.context = context;
        setLayout(new BorderLayout());
        setBackground(new Color(0xF4F6FA));
        setBorder(new EmptyBorder(36, 48, 36, 48));

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E7F1), 1, true),
                new EmptyBorder(30, 30, 30, 30)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Maintenance Mode");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 16f));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(20));

        toggle.setFocusPainted(false);
        toggle.setFont(toggle.getFont().deriveFont(Font.BOLD, 15f));
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        toggle.addActionListener(e -> updateMaintenance());
        card.add(toggle);

        card.add(Box.createVerticalStrut(12));
        JLabel help = new JLabel("<html>When ON, students and instructors can view but cannot modify data.</html>");
        help.setForeground(new Color(0x5A6A85));
        card.add(help);

        add(card, BorderLayout.NORTH);
        refreshState();
    }

    private void refreshState() {
        boolean enabled = context.maintenanceService.isMaintenanceMode();
        statusLabel.setText(enabled ? "Current state: ON" : "Current state: OFF");
        statusLabel.setForeground(enabled ? new Color(0xB54747) : new Color(0x1E8C5A));
        toggle.setSelected(enabled);
        toggle.setText(enabled ? "Disable Maintenance" : "Enable Maintenance");
    }

    private void updateMaintenance() {
        boolean enable = toggle.isSelected();
        int confirm = JOptionPane.showConfirmDialog(this,
                enable ? "Turn maintenance mode ON?" : "Turn maintenance mode OFF?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            toggle.setSelected(!enable);
            return;
        }
        context.adminService.toggleMaintenance(enable);
        refreshState();
        JOptionPane.showMessageDialog(this,
                enable ? "Maintenance mode enabled." : "Maintenance mode disabled.");
    }
}

