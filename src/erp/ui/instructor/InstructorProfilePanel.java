package erp.ui.instructor;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InstructorProfilePanel extends JPanel {

    private final int userId;

    private JLabel nameLbl;
    private JLabel deptLbl;
    private JLabel emailLbl;
    private JLabel phoneLbl;

    public InstructorProfilePanel(int userId) {
        this.userId = userId;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel card = createCard();
        add(card, BorderLayout.CENTER);

        loadProfile();
    }

    private JPanel createCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        InstructorDashboard.RoundedPanel tealBg =
                new InstructorDashboard.RoundedPanel(24, InstructorDashboard.TEAL);
        tealBg.setLayout(new BorderLayout());
        tealBg.setBorder(new EmptyBorder(12, 14, 14, 14));

        JLabel title = new JLabel("Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        tealBg.add(title, BorderLayout.NORTH);

        JPanel inner = new JPanel();
        inner.setBackground(Color.WHITE);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(20, 20, 20, 20));

        nameLbl = makeRow(inner, "Name:");
        deptLbl = makeRow(inner, "Department:");
        emailLbl = makeRow(inner, "Email:");
        phoneLbl = makeRow(inner, "Phone:");

        inner.add(Box.createVerticalStrut(25));

        tealBg.add(inner, BorderLayout.CENTER);
        outer.add(tealBg, BorderLayout.CENTER);

        return outer;
    }

    private JLabel makeRow(JPanel parent, String label) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setPreferredSize(new Dimension(120, 22));

        JLabel v = new JLabel("—");
        v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        v.setForeground(InstructorDashboard.TEXT_DARK);

        row.add(l);
        row.add(v);

        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(row);
        parent.add(Box.createVerticalStrut(8));

        return v;
    }

    private void styleButton(JButton b) {
        b.setBackground(InstructorDashboard.TEAL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    private void loadProfile() {
        String sql = "SELECT name, department, email, phone FROM instructors WHERE user_id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                nameLbl.setText(rs.getString("name"));
                deptLbl.setText(rs.getString("department"));
                emailLbl.setText(rs.getString("email"));
                phoneLbl.setText(rs.getString("phone"));
            } else {
                JOptionPane.showMessageDialog(this,
                        "Instructor profile not found.",
                        "Profile Error",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Unable to load profile.\nDetails: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
