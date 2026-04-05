package erp.ui;

import com.formdev.flatlaf.FlatLightLaf;
import erp.service.AuthResult;
import erp.service.AuthService;
import erp.ui.admin.AdminDashboard;
import erp.ui.instructor.InstructorDashboard;   // needed for instructor login
import erp.ui.student.StudentDashboard;             // needed for student login
import erp.ui.auth.ForgotPasswordFrame;


import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
public class LoginFrame extends JFrame {

    // ---- Brand palette (adjust if needed) ----
    private static final Color TEAL = new Color(0x2AB3A6);        // main teal for the card
    private static final Color TEAL_DARK = new Color(0x209387);
    private static final Color TEXT_DARK = new Color(0x1C1C1C);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color FIELD_UNDERLINE = new Color(255, 255, 255, 180);
    private static final Color CARD_BG = TEAL;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton eyeBtn;

    private Image rightImage;   // campus photo
    private Image logoImage;    // iiitd logo

    public LoginFrame() {
        // Window basics
        setTitle("Student ERP Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 640));

        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        // Load images
        try {
            rightImage = ImageIO.read(getClass().getResource("/erp/ui/iiitd_background.jpg"));
        } catch (Exception ignored) {}
        try {
            logoImage = ImageIO.read(getClass().getResource("/erp/ui/iiitd_logo.jpg"));
        } catch (Exception ignored) {}

        // Root layout: two columns
        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);

        // Right: full-bleed background image
        root.add(new ImagePanel(rightImage), BorderLayout.CENTER);

        // Left: vertical container with padding
        JPanel leftWrap = new JPanel(new GridBagLayout());
        leftWrap.setOpaque(true);
        leftWrap.setBackground(Color.WHITE); // subtle white like your screenshot
        leftWrap.setBorder(new EmptyBorder(24, 28, 24, 28));
        root.add(leftWrap, BorderLayout.WEST);

        // Fixed width for left column (so image stays visible)
        leftWrap.setPreferredSize(new Dimension(520, 0));

        // Build the teal card
        JPanel card = buildLoginCard();
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0; lc.gridy = 0;
        lc.fill = GridBagConstraints.NONE;
        lc.anchor = GridBagConstraints.NORTHWEST;
        leftWrap.add(card, lc);
    }

    // ---------------- UI Pieces ----------------

    private JPanel buildLoginCard() {
        RoundedPanel card = new RoundedPanel(28, CARD_BG);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(28, 28, 28, 28));
        card.setPreferredSize(new Dimension(460, 520));  // adjust numbers if you want
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 6, 6, 6);

        // Top logo + IIITD title
        JPanel brandRow = new JPanel(new GridBagLayout());
        brandRow.setOpaque(false);

        JLabel logo = new JLabel();
        if (logoImage != null) {
            Image scaled = logoImage.getScaledInstance(400, 100, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } else {
            logo.setText("IIITD");
            logo.setForeground(TEXT_LIGHT);
            logo.setFont(logo.getFont().deriveFont(Font.BOLD, 18f));
        }

//        JLabel inst = new JLabel("<html><div style='font-weight:600;'>INDRAPRASTHA INSTITUTE OF TECHNOLOGY <span style='color:#E3F6F3;'>DELHI</span></div></html>");
//        inst.setForeground(TEXT_LIGHT);
//        inst.setFont(inst.getFont().deriveFont(Font.PLAIN, 13f));

        GridBagConstraints br = new GridBagConstraints();
        br.insets = new Insets(0, 0, 0, 8); br.gridx = 0; br.anchor = GridBagConstraints.WEST;
        brandRow.add(logo, br);
        br.gridx = 1;
//        brandRow.add(inst, br);

        c.gridy = 0;
        card.add(brandRow, c);

        // Big heading
        JLabel heading = new JLabel("Sign in");
        heading.setForeground(TEXT_LIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 28f));
        c.gridy = 1; c.insets = new Insets(20, 6, 8, 6);
        card.add(heading, c);

        // Username label + field (underlined)
        JLabel uLabel = labelWhite("Username");
        c.gridy = 2; c.insets = new Insets(12, 6, 2, 6);
        card.add(uLabel, c);

        usernameField = new JTextField();
        styleUnderlinedField(usernameField);
        usernameField.putClientProperty("JTextField.placeholderText", "Enter username");
        c.gridy = 3; c.insets = new Insets(0, 6, 10, 6);
        card.add(usernameField, c);

        // Password label + field with eye
        JLabel pLabel = labelWhite("Password");
        c.gridy = 4; c.insets = new Insets(12, 6, 2, 6);
        card.add(pLabel, c);

        JPanel passRow = new JPanel(new BorderLayout());
        passRow.setOpaque(false);

        passwordField = new JPasswordField();
        styleUnderlinedField(passwordField);
        passwordField.putClientProperty("JPasswordField.placeholderText", "Enter password");

        eyeBtn = new JButton("\uD83D\uDC41"); // eye emoji
        eyeBtn.setOpaque(false);
        eyeBtn.setContentAreaFilled(false);
        eyeBtn.setBorderPainted(false);
        eyeBtn.setForeground(TEXT_LIGHT);
        eyeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeBtn.setToolTipText("Show/Hide Password");
        eyeBtn.addActionListener(e -> togglePassword());

        passRow.add(passwordField, BorderLayout.CENTER);
        passRow.add(eyeBtn, BorderLayout.EAST);

        c.gridy = 5; c.insets = new Insets(0, 6, 10, 6);
        card.add(passRow, c);

        // Sign In button
        JButton signIn = new JButton("SIGN IN");
        signIn.setFocusPainted(false);
        signIn.setFont(signIn.getFont().deriveFont(Font.BOLD, 14f));
        signIn.setForeground(TEXT_DARK);
        signIn.setBackground(Color.WHITE);
        signIn.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        signIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signIn.setOpaque(true);
        signIn.addActionListener(e -> doLogin());
        signIn.setPreferredSize(new Dimension(140, 44));

        RoundedPanel btnWrap = new RoundedPanel(18, new Color(255,255,255,200));
        btnWrap.setLayout(new BorderLayout());
        btnWrap.add(signIn, BorderLayout.CENTER);

        c.gridy = 6; c.insets = new Insets(14, 6, 10, 6);
        card.add(btnWrap, c);

        // Links
        JPanel links = new JPanel();
        links.setOpaque(false);
        links.setLayout(new GridLayout(2, 1, 0, 6));

        JLabel forgot = linkLike("Forgot Password");
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new ForgotPasswordFrame().setVisible(true);
            }
        });

        JLabel support = linkLike("Contact IT Support");
        support.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(LoginFrame.this,
                        "Please email it-support@iiitd.ac.in (dummy).",
                        "Contact IT Support", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        links.add(forgot);
        links.add(support);

        c.gridy = 7; c.insets = new Insets(10, 6, 4, 6);
        card.add(links, c);

        // Rounded corners shadow-ish bottom spacer
        c.gridy = 8; c.insets = new Insets(8, 6, 0, 6);
        card.add(Box.createVerticalStrut(6), c);

        return card;
    }

    private JLabel labelWhite(String txt) {
        JLabel l = new JLabel(txt);
        l.setForeground(TEXT_LIGHT);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 13f));
        return l;
    }

    private void styleUnderlinedField(JTextField f) {
        f.setOpaque(false);
        f.setForeground(TEXT_LIGHT);
        f.setCaretColor(TEXT_LIGHT);
        f.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, FIELD_UNDERLINE));
        f.setFont(f.getFont().deriveFont(Font.PLAIN, 14f));
        f.setMargin(new Insets(6, 2, 6, 2));
    }

    private JLabel linkLike(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(255,255,255,220));
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                l.setForeground(Color.WHITE);
            }
            @Override public void mouseExited(MouseEvent e)  {
                l.setForeground(new Color(255,255,255,220));
            }
        });
        return l;
    }

    private void togglePassword() {
        if (passwordField.getEchoChar() == 0) {
            passwordField.setEchoChar('•');
        } else {
            passwordField.setEchoChar((char) 0);
        }
        passwordField.repaint();
    }

    // ---------------- Helpers ----------------

    /** Simple rounded panel with a solid background. */
    static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bg;

        RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg;
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Image panel that scales the image to fill while keeping aspect. */
    static class ImagePanel extends JPanel {
        private final Image img;
        ImagePanel(Image img) { this.img = img; setOpaque(true); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img == null) {
                // fallback background
                g.setColor(new Color(245,245,245));
                g.fillRect(0,0,getWidth(),getHeight());
                return;
            }
            int iw = img.getWidth(this), ih = img.getHeight(this);
            if (iw <= 0 || ih <= 0) return;

            double pw = getWidth(), ph = getHeight();
            double scale = Math.max(pw / iw, ph / ih);
            int w = (int) Math.round(iw * scale);
            int h = (int) Math.round(ih * scale);
            int x = (int) ((pw - w) / 2);
            int y = (int) ((ph - h) / 2);

            // subtle vignette behind
            g.drawImage(img, x, y, w, h, this);
        }
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter details to continue!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AuthService auth = new AuthService();
        AuthResult result = auth.authenticate(username, password);

        if (result == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int userId = result.userId;
        switch (result.role.toUpperCase()) {
            case "ADMIN" -> {
                AdminDashboard a = new AdminDashboard();
                a.setVisible(true);
            }
            case "INSTRUCTOR" -> new InstructorDashboard(userId).setVisible(true);
            case "STUDENT" -> new StudentDashboard(userId).setVisible(true);
            default -> JOptionPane.showMessageDialog(this, "Unknown role: " + result.role);
        }
        dispose();
    }

    public static void main(String[] args) {
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}