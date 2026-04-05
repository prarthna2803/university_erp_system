package erp.ui.admin;

import erp.data.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddUserDialog extends JDialog {

    public AddUserDialog(AdminUsersPanel parent) {

        setTitle("Add New User");
        setModal(true);
        setSize(480, 380);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;

        // Stylish Header
        JLabel heading = new JLabel("Create New User");
        heading.setFont(new Font("SansSerif", Font.BOLD, 20));
        heading.setHorizontalAlignment(SwingConstants.CENTER);

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        panel.add(heading, c);

        // Reset gridwidth
        c.gridwidth = 1;

        // Input fields
        JTextField tfUsername = new JTextField();
        JTextField tfEmail = new JTextField();
        JPasswordField tfPassword = new JPasswordField();

        JComboBox<String> cbRole = new JComboBox<>(new String[]{"INSTRUCTOR", "STUDENT"});
        tfUsername.setPreferredSize(new Dimension(200, 28));
        tfEmail.setPreferredSize(new Dimension(200, 28));
        tfPassword.setPreferredSize(new Dimension(200, 28));

        // Username
        c.gridx = 0; c.gridy = 1;
        panel.add(new JLabel("Username:"), c);

        c.gridx = 1;
        panel.add(tfUsername, c);

        // Email
        c.gridx = 0; c.gridy = 2;
        panel.add(new JLabel("Email:"), c);

        c.gridx = 1;
        panel.add(tfEmail, c);

        // Password
        c.gridx = 0; c.gridy = 3;
        panel.add(new JLabel("Password:"), c);

        c.gridx = 1;
        panel.add(tfPassword, c);

        // Role
        c.gridx = 0; c.gridy = 4;
        panel.add(new JLabel("Role:"), c);

        c.gridx = 1;
        panel.add(cbRole, c);

        // Buttons panel
        JButton btnCancel = new JButton("Cancel");
        JButton btnSave = new JButton("Add User");

        // Make buttons consistent size
        btnCancel.setPreferredSize(new Dimension(110, 30));
        btnSave.setPreferredSize(new Dimension(110, 30));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        c.gridx = 0; c.gridy = 5; c.gridwidth = 2;
        panel.add(btnPanel, c);

        add(panel);

        // Cancel
        btnCancel.addActionListener(e -> dispose());

        // Save user
        btnSave.addActionListener(e -> {

            String username = tfUsername.getText().trim();
            String email = tfEmail.getText().trim();
            String password = new String(tfPassword.getPassword()).trim();
            String role = cbRole.getSelectedItem().toString();

            // Basic checks
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                return;
            }

            // Valid email check
            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid email format.\nMust end with '.com' or '.ac.in'",
                        "Email Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // Hash password
            String hash = BCrypt.hashpw(password, BCrypt.gensalt());

            try (Connection conn = DBConnection.getAuthConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO users_auth (username, email, password_hash, role, status) " +
                                 "VALUES (?, ?, ?, ?, 'ACTIVE')"
                 )) {

                ps.setString(1, username);
                ps.setString(2, email);
                ps.setString(3, hash);
                ps.setString(4, role);
                ps.executeUpdate();

                parent.refreshTable();
                dispose();

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                        this,
                        "Error adding user:\n" + ex.getMessage(),
                        "Database Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private boolean isValidEmail(String email) {
        return email.endsWith(".com") || email.endsWith(".ac.in");
    }
}
