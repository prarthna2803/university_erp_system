package erp.ui.admin;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminUsersPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    public AdminUsersPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Cyan rounded background
        JPanel wrapper = new RoundedPanel(24, new Color(0x00B3A4));
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Title and toolbar
        JLabel title = new JLabel("Users");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel toolbar = new JPanel();
        toolbar.setOpaque(false);

        JButton btnAdd = new JButton("Add");
        JButton btnEdit = new JButton("Edit");
        JButton btnDelete = new JButton("Delete");

        toolbar.add(btnAdd);
        toolbar.add(btnEdit);
        toolbar.add(btnDelete);

        top.add(title, BorderLayout.WEST);
        top.add(toolbar, BorderLayout.EAST);

        // Table setup
        model = new DefaultTableModel(new String[]{"User ID", "Username", "Role", "Email"}, 0);
        table = new JTable(model);

        refreshTable(); // load DB data

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);

        JPanel innerWhite = new JPanel(new BorderLayout());
        innerWhite.setBackground(Color.WHITE);
        innerWhite.setBorder(new EmptyBorder(10, 10, 10, 10));

        innerWhite.add(scroll, BorderLayout.CENTER);

        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(innerWhite, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);

        // Button actions
        btnAdd.addActionListener(e -> new AddUserDialog(this).setVisible(true));
        btnEdit.addActionListener(e -> editUser());
        btnDelete.addActionListener(e -> deleteUser());
    }

    public void refreshTable() {
        model.setRowCount(0);  // clear table

        String sql = "SELECT user_id, username, role, email FROM users_auth";

        try (Connection conn = DBConnection.getAuthConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("email")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // Edit user
    // ============================================================
    private void editUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user to edit.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);
        String username = (String) model.getValueAt(row, 1);
        String role = (String) model.getValueAt(row, 2);
        String email = (String) model.getValueAt(row, 3);

        new EditUserDialog(this, id, username, role, email).setVisible(true);
    }

    // ============================================================
    // Delete user
    // ============================================================
    private void deleteUser() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a user to delete.");
            return;
        }

        int id = (int) model.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this user?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM users_auth WHERE user_id = ?")) {

            ps.setInt(1, id);
            ps.executeUpdate();
            refreshTable();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Rounded panel
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
        }
    }
}
