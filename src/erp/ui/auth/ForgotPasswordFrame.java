package erp.ui.auth;

import erp.data.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;

public class ForgotPasswordFrame extends JFrame {

    private JTextField usernameField;
    private JTextField emailField;

    public ForgotPasswordFrame() {
        setTitle("Forgot Password");
        setSize(420, 220);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        usernameField = new JTextField();
        emailField = new JTextField();
        JButton sendBtn = new JButton("Generate Reset Token");
        sendBtn.addActionListener(e -> handleSendToken());

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0 – Username
        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Username:"), gc);

        gc.gridx = 1;
        panel.add(usernameField, gc);

        // Row 1 – Email
        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("Registered Email:"), gc);

        gc.gridx = 1;
        panel.add(emailField, gc);

        // Row 2 – Button spanning two columns
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2;
        panel.add(sendBtn, gc);

        add(panel);
    }

    private void handleSendToken() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();

        if (username.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        try (Connection conn = DBConnection.getAuthConnection()) {

            // 1) Find user
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM users_auth WHERE username = ? AND email = ?"
            );
            ps.setString(1, username);
            ps.setString(2, email);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this,
                        "No user found with that username + email.");
                return;
            }
            int userId = rs.getInt("user_id");

            // 2) Generate 5-digit numeric token + expiry
            SecureRandom rnd = new SecureRandom();
            int tokenNum = 10000 + rnd.nextInt(90000); // 10000–99999
            String token = String.valueOf(tokenNum);

            LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);

            PreparedStatement ins = conn.prepareStatement(
                    "INSERT INTO password_resets (user_id, token, expires_at) VALUES (?, ?, ?)"
            );
            ins.setInt(1, userId);
            ins.setString(2, token);
            ins.setTimestamp(3, Timestamp.valueOf(expiry));
            ins.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Your reset token is:\n" + token + "\n(Valid for 15 minutes)");

            // Open reset window
            new ResetPasswordFrame().setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
