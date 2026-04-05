package erp.ui.auth;

import erp.data.DBConnection;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;

public class ResetPasswordFrame extends JFrame {

    private JTextField tokenField;
    private JPasswordField newPassField;
    private JPasswordField confirmPassField;

    public ResetPasswordFrame() {
        setTitle("Reset Password");
        setSize(420, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // ---- MAIN PANEL ----
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(10, 10, 10, 10);
        gc.fill = GridBagConstraints.HORIZONTAL;

        tokenField = new JTextField(15);
        newPassField = new JPasswordField(15);
        confirmPassField = new JPasswordField(15);

        // ---- ROW 0 ----
        gc.gridx = 0; gc.gridy = 0;
        panel.add(new JLabel("Reset Token:"), gc);

        gc.gridx = 1;
        panel.add(tokenField, gc);

        // ---- ROW 1 ----
        gc.gridx = 0; gc.gridy = 1;
        panel.add(new JLabel("New Password:"), gc);

        gc.gridx = 1;
        panel.add(newPassField, gc);

        // ---- ROW 2 ----
        gc.gridx = 0; gc.gridy = 2;
        panel.add(new JLabel("Confirm Password:"), gc);

        gc.gridx = 1;
        panel.add(confirmPassField, gc);

        // ---- ROW 3 (Button) ----
        JButton resetBtn = new JButton("Reset Password");
        resetBtn.addActionListener(e -> handleReset());

        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 2;
        panel.add(resetBtn, gc);

        add(panel);
    }

    private void handleReset() {
        String token = tokenField.getText().trim();
        String pass = new String(newPassField.getPassword());
        String confirm = new String(confirmPassField.getPassword());

        if (token.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.");
            return;
        }

        if (!pass.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        try (Connection conn = DBConnection.getAuthConnection()) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id, expires_at FROM password_resets WHERE token = ?"
            );
            ps.setString(1, token);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Invalid token.");
                return;
            }

            Timestamp exp = rs.getTimestamp("expires_at");
            if (exp.toLocalDateTime().isBefore(LocalDateTime.now())) {
                JOptionPane.showMessageDialog(this, "Token expired.");
                return;
            }

            int userId = rs.getInt("user_id");
            String hash = BCrypt.hashpw(pass, BCrypt.gensalt());

            PreparedStatement upd = conn.prepareStatement(
                    "UPDATE users_auth SET password_hash = ? WHERE user_id = ?"
            );
            upd.setString(1, hash);
            upd.setInt(2, userId);
            upd.executeUpdate();

            PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM password_resets WHERE token = ?"
            );
            del.setString(1, token);
            del.executeUpdate();

            JOptionPane.showMessageDialog(this, "Password reset successful!");
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
