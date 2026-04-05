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

public class StudentCoursesPanel extends JPanel {

    private final StudentDashboard dashboard;
    private final int userId;

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    public StudentCoursesPanel(StudentDashboard dashboard, int userId) {
        this.dashboard = dashboard;
        this.userId = userId;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel wrapper = new RoundedPanel(24, new Color(0x00B3A4));
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(14, 14, 14, 14));

        wrapper.add(buildTopBar(), BorderLayout.NORTH);
        wrapper.add(buildInnerTable(), BorderLayout.CENTER);
        wrapper.add(buildBottomBar(), BorderLayout.SOUTH);

        add(wrapper, BorderLayout.CENTER);

        loadCatalogData();
    }

    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 2, 10, 2));

        JLabel title = new JLabel("Browse Course Catalog");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        searchField = new JTextField(20);
        searchField.addCaretListener(e -> applyFilter());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(Color.WHITE);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        top.add(title, BorderLayout.WEST);
        top.add(searchPanel, BorderLayout.EAST);

        return top;
    }

    private JComponent buildInnerTable() {
        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Color.WHITE);
        inner.setBorder(new EmptyBorder(10, 10, 10, 10));
        inner.add(buildTable(), BorderLayout.CENTER);
        return inner;
    }

    private JComponent buildTable() {

        String[] cols = {
                "SectionID", "CourseID", "CapacityNum", "EnrolledNum",
                "Code", "Title", "Credits", "Section",
                "Enrolled/Cap", "Instructor", "Day & Time",
                "Room", "Semester", "Year",
                "Reg Deadline", "Drop Deadline"
        };

        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(22);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Hide internal columns
        for (int i = 0; i < 4; i++)
            table.removeColumn(table.getColumnModel().getColumn(0));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        return scroll;
    }

    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton btn = new JButton("Register in Selected Section");
        btn.setBackground(new Color(0x00B3A4));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);

        btn.addActionListener(e -> handleRegister());

        if (dashboard.isMaintenanceOn()) {
            btn.setEnabled(false);
            btn.setToolTipText("Disabled due to maintenance mode.");
        }

        bottom.add(btn);
        return bottom;
    }

    private void applyFilter() {
        String text = searchField.getText();
        if (text.isBlank()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
    }

    public void loadCatalogData() {
        model.setRowCount(0);

        String sql = """
            SELECT 
                t.id AS section_id,
                c.course_id AS course_id,
                c.code,
                c.title,
                c.credits,
                c.instructor,
                t.capacity,
                t.day,
                t.start_time,
                t.end_time,
                t.room,
                t.semester,
                t.year,
                c.reg_deadline,
                c.drop_deadline,
                (SELECT COUNT(*) FROM registrations r WHERE r.course_id = c.course_id) AS enrolled
            FROM timetable t
            JOIN courses c ON t.course_id = c.course_id
            ORDER BY c.code, t.day, t.start_time
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                int sectionId = rs.getInt("section_id");
                int courseId = rs.getInt("course_id");
                int capacity = rs.getInt("capacity");
                int enrolled = rs.getInt("enrolled");

                String dayTime = rs.getString("day") + " " +
                        rs.getString("start_time") + "–" + rs.getString("end_time");

                model.addRow(new Object[]{
                        sectionId,
                        courseId,
                        capacity,
                        enrolled,
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        "SEC-" + sectionId,
                        enrolled + " / " + capacity,
                        rs.getString("instructor"),
                        dayTime,
                        rs.getString("room"),
                        rs.getString("semester"),
                        rs.getInt("year"),
                        rs.getDate("reg_deadline"),
                        rs.getDate("drop_deadline")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load catalog: " + e.getMessage());
        }
    }

    public void reload() {
        loadCatalogData();
    }

    private void handleRegister() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a section.");
            return;
        }

        if (dashboard.isMaintenanceOn()) {
            JOptionPane.showMessageDialog(this,
                    "You cannot register during Maintenance Mode.");
            return;
        }

        int row = table.convertRowIndexToModel(viewRow);

        int courseId = (Integer) model.getValueAt(row, 1);
        int capacity = (Integer) model.getValueAt(row, 2);
        int enrolled = (Integer) model.getValueAt(row, 3);

        String code = model.getValueAt(row, 4).toString();
        String title = model.getValueAt(row, 5).toString();

        if (isAlreadyEnrolled(courseId)) {
            JOptionPane.showMessageDialog(this, "You are already registered in this course.");
            return;
        }

        if (enrolled >= capacity) {
            JOptionPane.showMessageDialog(this, "Section full.");
            return;
        }

        if (isPastDeadline(courseId)) {
            JOptionPane.showMessageDialog(this, "Registration deadline has passed.");
            return;
        }

        if (insertEnrollment(courseId)) {
            enrolled++;
            model.setValueAt(enrolled, row, 3);
            model.setValueAt(enrolled + " / " + capacity, row, 8);

            JOptionPane.showMessageDialog(this,
                    "Successfully registered in " + code + " – " + title);
        }
    }

    private boolean isAlreadyEnrolled(int courseId) {
        String sql = "SELECT COUNT(*) FROM registrations WHERE user_id = ? AND course_id = ?";
        try (Connection c = DBConnection.getERPConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;

        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private boolean isPastDeadline(int courseId) {
        String sql = "SELECT reg_deadline FROM courses WHERE course_id = ?";
        try (Connection c = DBConnection.getERPConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                var d = rs.getDate("reg_deadline");
                return d != null && java.time.LocalDate.now().isAfter(d.toLocalDate());
            }

        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private boolean insertEnrollment(int courseId) {
        String sql = "INSERT INTO registrations (user_id, course_id) VALUES (?, ?)";
        try (Connection c = DBConnection.getERPConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, courseId);
            return ps.executeUpdate() == 1;

        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    static class RoundedPanel extends JPanel {
        int arc;
        Color bg;
        RoundedPanel(int arc, Color bg) { this.arc = arc; this.bg = bg; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
        }
    }
}
