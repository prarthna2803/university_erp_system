package erp.ui.student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StudentFeesPanel extends JPanel {

    public StudentFeesPanel(StudentDashboard dashboard) {
        setLayout(new BorderLayout());
        setBackground(StudentDashboard.BG_WHITE);
        setBorder(new EmptyBorder(0, 18, 18, 18));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        add(wrapper, BorderLayout.CENTER);

        JLabel title = new JLabel("Fees");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(StudentDashboard.TEXT_DARK);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        wrapper.add(title);

        // Due Amount card
        JPanel dueContent = new JPanel(new BorderLayout());
        dueContent.setBackground(Color.WHITE);

        JLabel dueLabel = new JLabel(" ₹ 0.00 ", SwingConstants.LEFT);
        dueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dueContent.add(dueLabel, BorderLayout.CENTER);

        JButton payButton = new JButton("Pay");
        payButton.setBackground(StudentDashboard.TEAL);
        payButton.setForeground(Color.WHITE);
        payButton.setFocusPainted(false);
        payButton.setBorder(new EmptyBorder(4, 16, 4, 16));
        dueContent.add(payButton, BorderLayout.EAST);

        wrapper.add(dashboard.createCard("Due Amount", dueContent));
        wrapper.add(Box.createVerticalStrut(16));

        // Payment history card
        JTextArea historyArea = new JTextArea("No payments recorded yet.");
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        historyArea.setBorder(new EmptyBorder(4, 6, 4, 6));

        JScrollPane histScroll = new JScrollPane(historyArea);
        histScroll.setBorder(null);

        wrapper.add(dashboard.createCard("Payment History", histScroll));
    }
}
