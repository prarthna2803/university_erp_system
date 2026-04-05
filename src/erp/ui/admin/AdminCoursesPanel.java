package erp.ui.admin;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminCoursesPanel extends JPanel {

    public JTable table;
    public DefaultTableModel model;

    public AdminCoursesPanel() {

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel wrapper = new RoundedPanel(24, new Color(0x00B3A4));
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("Courses Administration");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);

        JButton btnAdd = new JButton("Add");
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);

        top.add(toolbar, BorderLayout.EAST);

        model = new DefaultTableModel(new String[]{
                "SectionID", "CourseID", "Code", "Title", "Credits",
                "Instructor", "Day & Time", "Room", "Capacity",
                "Semester", "Year", "Reg Deadline", "Drop Deadline"
        }, 0);

        table = new JTable(model);
        table.setRowHeight(22);
        JScrollPane scroll = new JScrollPane(table);

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Color.WHITE);
        inner.add(scroll);

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(inner, BorderLayout.CENTER);

        add(wrapper);

        btnAdd.addActionListener(e -> new AddCourseDialog(this).setVisible(true));
        btnEdit.addActionListener(e -> editSelected());
        btnDelete.addActionListener(e -> deleteSelected());

        loadCourses();
    }

    public void loadCourses() {
        model.setRowCount(0);

        String sql = """
            SELECT 
                t.id AS section_id,
                c.course_id,
                c.code,
                c.title,
                c.credits,
                c.instructor,
                CONCAT(t.day, ' ', t.start_time, '–', t.end_time) AS daytime,
                t.room,
                t.capacity,
                t.semester,
                t.year,
                c.reg_deadline,
                c.drop_deadline
            FROM timetable t
            JOIN courses c ON t.course_id = c.course_id
            ORDER BY c.code;
        """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                model.addRow(new Object[]{
                        rs.getInt("section_id"),
                        rs.getInt("course_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        rs.getString("instructor"),
                        rs.getString("daytime"),
                        rs.getString("room"),
                        rs.getInt("capacity"),
                        rs.getString("semester"),
                        rs.getInt("year"),
                        rs.getDate("reg_deadline"),
                        rs.getDate("drop_deadline")
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a course.");
            return;
        }

        new EditCourseDialog(
                this,
                (int) model.getValueAt(row, 0), // section_id
                (int) model.getValueAt(row, 1), // course_id
                (String) model.getValueAt(row, 2),
                (String) model.getValueAt(row, 3),
                (int) model.getValueAt(row, 4),
                (String) model.getValueAt(row, 5),
                (String) model.getValueAt(row, 6),
                (String) model.getValueAt(row, 7),
                (int) model.getValueAt(row, 8),
                (String) model.getValueAt(row, 9),
                (int) model.getValueAt(row, 10),
                model.getValueAt(row, 11).toString(),
                model.getValueAt(row, 12).toString()
        ).setVisible(true);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int sectionId = (int) model.getValueAt(row, 0);
        int courseId = (int) model.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(
                this, "Delete this course permanently?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getERPConnection()) {

            PreparedStatement tdel = conn.prepareStatement("DELETE FROM timetable WHERE id=?");
            tdel.setInt(1, sectionId);
            tdel.executeUpdate();

            PreparedStatement cdel = conn.prepareStatement("DELETE FROM courses WHERE course_id=?");
            cdel.setInt(1, courseId);
            cdel.executeUpdate();

            loadCourses();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static class RoundedPanel extends JPanel {
        int arc;
        Color bg;

        RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg;
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
        }
    }
}
