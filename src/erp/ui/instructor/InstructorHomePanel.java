package erp.ui.instructor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class InstructorHomePanel extends JPanel {

    InstructorHomePanel(InstructorContext context) {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF5F7FB));
        setBorder(new EmptyBorder(40, 60, 40, 60));

        JPanel card = new JPanel();
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E7F1), 1, true),
                new EmptyBorder(32, 32, 32, 32)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Hello, " + context.username);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(0x142033));
        card.add(title);

        card.add(Box.createVerticalStrut(12));
        JLabel subtitle = new JLabel("Manage your sections, record assessments, and review analytics.");
        subtitle.setForeground(new Color(0x5A6A85));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        card.add(subtitle);

        if (context.maintenanceService.isMaintenanceMode()) {
            card.add(Box.createVerticalStrut(24));
            JLabel banner = new JLabel("Maintenance mode is active: editing is temporarily disabled.");
            banner.setForeground(new Color(0xB54747));
            banner.setFont(new Font("Segoe UI", Font.BOLD, 14));
            card.add(banner);
        }

        add(card, BorderLayout.CENTER);
    }
}

