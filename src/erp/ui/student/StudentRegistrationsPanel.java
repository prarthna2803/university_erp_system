package erp.ui.student;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentRegistrationsPanel extends JPanel {

    private final StudentDashboard dashboard;
    private final int userId;

    private JTable table;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public StudentRegistrationsPanel(StudentDashboard dashboard, int userId) {
        this.dashboard = dashboard;
        this.userId = userId;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // ---- WRAPPER (Same teal rounded card as grades panel) ----
        JPanel wrapper = new RoundedPanel(24, new Color(0x00B3A4));
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(14, 14, 14, 14));

        wrapper.add(buildTopBar(), BorderLayout.NORTH);
        wrapper.add(buildInnerTable(), BorderLayout.CENTER);
        wrapper.add(buildBottomBar(), BorderLayout.SOUTH);

        add(wrapper, BorderLayout.CENTER);

        loadRegistrations();
    }

    // ---------------------------------------------------------
    // TOP BAR (same as grades)
    // ---------------------------------------------------------
    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 2, 10, 2));

        JLabel title = new JLabel("My Registrations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        top.add(title, BorderLayout.WEST);
        return top;
    }

    // ---------------------------------------------------------
    // INNER WHITE TABLE PANEL (same as grades)
    // ---------------------------------------------------------
    private JComponent buildInnerTable() {
        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Color.WHITE);
        inner.setBorder(new EmptyBorder(10, 10, 10, 10));
        inner.add(buildTable(), BorderLayout.CENTER);
        return inner;
    }

    private JComponent buildTable() {

        String[] cols = {
                "Course ID",      // hidden
                "Course Code",
                "Course Title",
                "Credits",
                "Day & Time",
                "Room",
                "Status"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(22);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Hide Course ID column (0)
        table.removeColumn(table.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(null);

        return scroll;
    }

    // ---------------------------------------------------------
    // BOTTOM BAR (same styling as grades)
    // ---------------------------------------------------------
    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton btnCatalog = new JButton("Register New (Open Catalog)");
        styleButton(btnCatalog);
        btnCatalog.addActionListener(e ->
                dashboard.cardLayout.show(dashboard.contentPanel, "COURSES")
        );

        JButton btnDrop = new JButton("Drop Section");
        styleButton(btnDrop);
        btnDrop.addActionListener(e -> handleDrop());

        bottom.add(btnCatalog);
        bottom.add(btnDrop);
        return bottom;
    }

    // ---- Apply Teal Button Style ----
    private void styleButton(JButton btn) {
        btn.setBackground(new Color(0x00B3A4));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ---------------------------------------------------------
    // LOAD REGISTRATIONS (NO CHANGE)
    // ---------------------------------------------------------

    private void loadRegistrations() {
        model.setRowCount(0);

        String sql = """
            SELECT 
                r.course_id,
                c.code,
                c.title,
                c.credits,
                t.day,
                t.start_time,
                t.end_time,
                t.room
            FROM registrations r
            JOIN courses c ON r.course_id = c.course_id
            LEFT JOIN timetable t ON t.course_id = r.course_id
            WHERE r.user_id = ?
            ORDER BY c.code
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                int courseId = rs.getInt("course_id");
                String code  = rs.getString("code");
                String title = rs.getString("title");
                int credits  = rs.getInt("credits");

                String day   = rs.getString("day");
                String start = rs.getString("start_time");
                String end   = rs.getString("end_time");
                String room  = rs.getString("room");

                String dayTime =
                        (day == null || start == null || end == null)
                                ? "TBA"
                                : day + " " + start + "–" + end;

                if (room == null) room = "TBA";

                model.addRow(new Object[]{
                        courseId,
                        code,
                        title,
                        credits,
                        dayTime,
                        room,
                        "Active"
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to load registrations: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------
    // DROP LOGIC (NO CHANGE)
    // ---------------------------------------------------------
    private void handleDrop() {

        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a registration to drop.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (dashboard.isMaintenanceOn()) {
            JOptionPane.showMessageDialog(this,
                    "Cannot drop during Maintenance Mode.",
                    "Maintenance", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        int courseId = (Integer) model.getValueAt(modelRow, 0);
        String code  = model.getValueAt(modelRow, 1).toString();
        String title = model.getValueAt(modelRow, 2).toString();

        if (isDropDeadlinePassed(courseId)) {
            JOptionPane.showMessageDialog(this,
                    "Drop deadline has passed.",
                    "Deadline", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Drop " + code + " – " + title + "?",
                "Confirm Drop", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        if (deleteEnrollment(courseId)) {
            JOptionPane.showMessageDialog(this,
                    "Successfully dropped " + code + " – " + title + ".",
                    "Dropped", JOptionPane.INFORMATION_MESSAGE);
            loadRegistrations();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Drop failed. Try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isDropDeadlinePassed(int courseId) {
        String sql = "SELECT reg_deadline FROM courses WHERE course_id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                java.sql.Date d = rs.getDate("reg_deadline");
                if (d == null) return false;
                return java.time.LocalDate.now().isAfter(d.toLocalDate());
            }

        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private boolean deleteEnrollment(int courseId) {
        String sql = "DELETE FROM registrations WHERE user_id = ? AND course_id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() > 0;

        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public void reload() {
        loadRegistrations();
    }

    // ---------------------------------------------------------
    // ROUNDED PANEL (same as grades)
    // ---------------------------------------------------------
    static class RoundedPanel extends JPanel {
        int arc;
        Color bg;
        RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
        }
    }
}
