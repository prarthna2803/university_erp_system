package erp.ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class AdminMorePanel extends JPanel {

    public AdminMorePanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JLabel msg = new JLabel("More settings will appear here.", SwingConstants.CENTER);
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        msg.setBorder(new EmptyBorder(40, 10, 40, 10));

        add(msg, BorderLayout.CENTER);
    }
}
