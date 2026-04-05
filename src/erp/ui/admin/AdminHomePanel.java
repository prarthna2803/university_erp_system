package erp.ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminHomePanel extends JPanel {

    public AdminHomePanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(40, 40, 40, 40));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Welcome, Administrator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Use the left panel to manage users, courses, or system settings.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(10, 0, 0, 0));

        content.add(title);
        content.add(subtitle);

        add(content, BorderLayout.NORTH);
    }
}
