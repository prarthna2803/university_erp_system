package erp.ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentHostelPanel extends JPanel {

    public StudentHostelPanel(StudentDashboard dashboard) {
        setLayout(new BorderLayout());
        setBackground(StudentDashboard.BG_WHITE);
        setBorder(new EmptyBorder(0, 18, 18, 18));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        add(wrapper, BorderLayout.CENTER);

        JLabel title = new JLabel("Hostel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(StudentDashboard.TEXT_DARK);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        wrapper.add(title);

        JPanel hostelCard = dashboard.createCard(
                "Hostel Information",
                new JLabel(
                        "Hostel details and room allocation will be shown here.",
                        SwingConstants.CENTER
                )
        );
        wrapper.add(hostelCard);
    }
}
