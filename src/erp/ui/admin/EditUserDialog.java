package erp.ui.admin;

import erp.data.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditUserDialog extends JDialog {

    public EditUserDialog(AdminUsersPanel parent, int userId, String username, String role, String email) {

        setTitle("Edit User");
        setModal(true);
        setSize(450, 300);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField tfUsername = new JTextField(username);
        JTextField tfEmail = new JTextField(email);

        JComboBox<String> cbRole = new JComboBox<>(new String[]{"INSTRUCTOR", "STUDENT"});
        cbRole.setSelectedItem(role);

        // Username
        c.gridx = 0; c.gridy = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        panel.add(tfUsername, c);

        // Email
        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Email:"), c);
        c.gridx = 1;
        panel.add(tfEmail, c);

        // Role
        c.gridx = 0; c.gridy = 2;
        panel.add(new JLabel("Role:"), c);
        c.gridx = 1;
        panel.add(cbRole, c);

        // Buttons
        JButton btnCancel = new JButton("Cancel");
        JButton btnSave = new JButton("Save");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        panel.add(btnPanel, c);

        add(panel);

        btnCancel.addActionListener(e -> dispose());

        btnSave.addActionListener(e -> {

            String newName = tfUsername.getText().trim();
            String newEmail = tfEmail.getText().trim();
            String newRole = cbRole.getSelectedItem().toString();

            // Validate email
            if (!isValidEmail(newEmail)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid email! Must end with '.com' or '.ac.in'",
                        "Email Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            try (Connection conn = DBConnection.getAuthConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE users_auth SET username=?, email=?, role=? WHERE user_id=?"
                 )) {

                ps.setString(1, newName);
                ps.setString(2, newEmail);
                ps.setString(3, newRole);
                ps.setInt(4, userId);
                ps.executeUpdate();

                parent.refreshTable();
                dispose();

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error updating user.", "Database Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email.endsWith(".com") || email.endsWith(".ac.in");
    }
}
