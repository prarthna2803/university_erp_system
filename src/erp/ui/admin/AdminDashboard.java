package erp.ui.admin;

import erp.ui.LoginFrame;
import erp.data.DBConnection;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class AdminDashboard extends JFrame {

    public static final Color BG_WHITE = Color.WHITE;
    public static final Color SIDEBAR_BG = new Color(0xDDE3E6);
    public static final Color TEAL_ACTIVE = new Color(0x00BFBF);
    public static final Color TEXT_DARK = new Color(0x222222);

    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JPanel footerBanner;

    private ToggleSwitch maintenanceToggle;

    private JButton btnHome, btnUsers, btnCourses, btnMore;
    private Image logoImage;

    public AdminDashboard() {

        setTitle("Admin Dashboard");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        setContentPane(root);
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        root.add(centerWrapper, BorderLayout.CENTER);

        // --- IMPORTANT: build footer BEFORE using it ---
        footerBanner = buildMaintenanceBanner();
        root.add(footerBanner, BorderLayout.SOUTH);

        // now it's safe to use it
        footerBanner.setVisible(isMaintenanceOn());

        // header AFTER footer exists
        centerWrapper.add(buildHeader(), BorderLayout.NORTH);

        // pages
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        centerWrapper.add(contentPanel, BorderLayout.CENTER);

        contentPanel.add(new AdminHomePanel(), "HOME");
        contentPanel.add(new AdminUsersPanel(), "USERS");
        contentPanel.add(new AdminCoursesPanel(), "COURSES");
        contentPanel.add(new AdminMorePanel(), "MORE");

        setVisible(true);
    }


    private JPanel buildSidebar() {

        JPanel side = new JPanel();
        side.setBackground(SIDEBAR_BG);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(160, 0));
        side.setBorder(new EmptyBorder(16, 10, 16, 10));

        btnHome = makeSidebarButton("Home");
        btnUsers = makeSidebarButton("Manage Users");
        btnCourses = makeSidebarButton("Manage Courses");
        btnMore = makeSidebarButton("More");

        side.add(btnHome);
        side.add(Box.createVerticalStrut(10));
        side.add(btnUsers);
        side.add(Box.createVerticalStrut(10));
        side.add(btnCourses);
        side.add(Box.createVerticalStrut(10));
        side.add(btnMore);

        side.add(Box.createVerticalGlue());

        // Logout button
        JButton logout = new JButton("Logout");
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setBackground(TEXT_DARK);
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.setBorder(new EmptyBorder(6, 10, 6, 10));

        logout.addActionListener(e -> {
            dispose();
            new LoginFrame().setVisible(true);
        });

        side.add(logout);

        return side;
    }

    private JButton makeSidebarButton(String label) {
        JButton b = new JButton(label);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setBackground(Color.WHITE);
        b.setForeground(TEXT_DARK);

        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC0C7CC)),
                new EmptyBorder(6, 10, 6, 10)
        ));

        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addActionListener(e -> {
            setActiveButton(b);
            cardLayout.show(contentPanel, label.equals("Home") ? "HOME" :
                    label.equals("Manage Users") ? "USERS" :
                            label.equals("Manage Courses") ? "COURSES" :
                                    "MORE");
        });

        return b;
    }

    private void setActiveButton(JButton active) {
        JButton[] all = {btnHome, btnUsers, btnCourses, btnMore};
        for (JButton b : all) {
            b.setBackground(Color.WHITE);
            b.setForeground(TEXT_DARK);
        }
        active.setBackground(TEAL_ACTIVE);
        active.setForeground(Color.WHITE);
    }
    private JPanel buildHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_WHITE);
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel bannerLabel = new JLabel();

        try {
            Image banner = ImageIO.read(getClass().getResource("/erp/ui/iiit_banner.jpg"));
            Image scaledBanner = banner.getScaledInstance(450, 80, Image.SCALE_SMOOTH);
            bannerLabel.setIcon(new ImageIcon(scaledBanner));
        } catch (Exception ignored) {
            bannerLabel.setText("IIITD");
            bannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        }

        header.add(bannerLabel, BorderLayout.WEST);

        JPanel maintenancePanel = new JPanel();
        maintenancePanel.setOpaque(true);
        maintenancePanel.setBackground(new Color(0xD0D5D6));
        maintenancePanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        maintenancePanel.setLayout(new BoxLayout(maintenancePanel, BoxLayout.X_AXIS));

        JLabel maintenanceLabel = new JLabel("Maintenance mode");
        maintenanceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        maintenanceLabel.setBorder(new EmptyBorder(0, 0, 0, 15));

        maintenanceToggle = new ToggleSwitch();
        maintenanceToggle.setPreferredSize(new Dimension(55, 28));

        maintenancePanel.add(maintenanceLabel);
        maintenancePanel.add(maintenanceToggle);

        // Toggle behavior
        maintenanceToggle.addSwitchListener(isOn -> {

            if (isOn) {
                int choice = JOptionPane.showOptionDialog(
                        this,
                        "Entering maintenance mode prevents STUDENTS & INSTRUCTORS\n"
                                + "from making changes.\n\nContinue?",
                        "Enable Maintenance Mode",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new String[]{"Continue", "Cancel"},
                        "Cancel"
                );

                if (choice == 0) {
                    footerBanner.setVisible(true);
                    updateMaintenanceMode(true);   // <-- SAVE TO DB
                } else {
                    maintenanceToggle.setState(false);
                }

            } else {
                footerBanner.setVisible(false);
                updateMaintenanceMode(false);      // <-- SAVE TO DB
            }
        });
        header.add(maintenancePanel, BorderLayout.EAST);
        return header;
    }

    // Save maintenance mode state to database
    private void updateMaintenanceMode(boolean isOn) {
        String sql = "UPDATE settings SET value = ? WHERE `key` = 'maintenance_mode'";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, isOn ? "ON" : "OFF");
            ps.executeUpdate();

            System.out.println("Maintenance mode updated to: " + (isOn ? "ON" : "OFF"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean loadMaintenanceMode() {
        String sql = "SELECT value FROM settings WHERE `key` = 'maintenance_mode'";

        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getString("value").equalsIgnoreCase("ON");
            }

        } catch (Exception e) { e.printStackTrace(); }

        return false; // default if anything fails
    }

    public boolean isMaintenanceOn() {
        String sql = "SELECT value FROM settings WHERE `key`='maintenance_mode'";
        try (Connection conn = DBConnection.getERPConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getString("value").equalsIgnoreCase("ON");
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    private JPanel buildMaintenanceBanner() {

        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(255, 235, 130));

        JLabel msg = new JLabel(
                " ⚠ ERP is in Maintenance Mode — Students & Instructors cannot make changes.",
                SwingConstants.CENTER
        );
        msg.setForeground(Color.RED);
        msg.setFont(new Font("Segoe UI", Font.BOLD, 14));

        banner.add(msg, BorderLayout.CENTER);
        return banner;
    }
}