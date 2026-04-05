package erp.ui.student;

import erp.data.DBConnection;
import erp.ui.LoginFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public class StudentDashboard extends JFrame {

    public final int userId;
    public String username;

    // course tables (shared with panels)
    public JTable tableCourses;
    public JTable tableRegistered;

    // brand colors
    public static final Color BG_WHITE   = Color.WHITE;
    public static final Color SIDEBAR_BG = new Color(0xDDE3E6);
    public static final Color TEAL       = new Color(0x00B3A4);
    public static final Color TEAL_DARK  = new Color(0x009688);
    public static final Color TEXT_DARK  = new Color(0x222222);

    public Image logoImage;
    public CardLayout cardLayout;
    public JPanel contentPanel;
    private JPanel maintenanceBanner;


    public StudentCoursesPanel coursesPanel;
    public StudentRegistrationsPanel regsPanel;
    public StudentTimetablePanel timetablePanel;
    public StudentHomePanel homePanel;
    private final StudentGradesPanel gradesPanel;

    public StudentDashboard(int userId) {
        this.userId = userId;
        this.username = loadUsername();

        setTitle("Student Dashboard");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // load logo
        try {
            logoImage = ImageIO.read(getClass().getResource("/erp/ui/iiit_banner.jpg"));
        } catch (Exception e) {
            System.out.println("StudentDashboard: logo not found");
        }

        loadAvailableCourses();
        loadRegisteredCourses();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_WHITE);
        setContentPane(root);
        root.add(buildSidebar(), BorderLayout.WEST);
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BG_WHITE);
        root.add(centerWrapper, BorderLayout.CENTER);
        centerWrapper.add(buildHeader(), BorderLayout.NORTH);
        maintenanceBanner = buildMaintenanceBanner();
        root.add(maintenanceBanner, BorderLayout.SOUTH);
        maintenanceBanner.setVisible(isMaintenanceOn());

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        centerWrapper.add(contentPanel, BorderLayout.CENTER);
        homePanel        = new StudentHomePanel(this);
        coursesPanel     = new StudentCoursesPanel(this, userId);
        regsPanel        = new StudentRegistrationsPanel(this, userId);
        timetablePanel   = new StudentTimetablePanel(this, userId);
        gradesPanel = new StudentGradesPanel(this, userId);

        contentPanel.add(homePanel, "HOME");
        contentPanel.add(new StudentGradesPanel(this, userId), "GRADES");
        contentPanel.add(coursesPanel, "COURSES");
        contentPanel.add(regsPanel, "REGISTRATIONS");
        contentPanel.add(timetablePanel, "TIMETABLE");
        contentPanel.add(new StudentFeesPanel(this), "FEES");
        contentPanel.add(new StudentHostelPanel(this), "HOSTEL");
        contentPanel.add(new StudentReportsPanel(this), "REPORTS");

        cardLayout.show(contentPanel, "HOME");
        setVisible(true);
    }

    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(SIDEBAR_BG);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(160, 0));
        side.setBorder(new EmptyBorder(16, 10, 16, 10));

        side.add(sideButton("Home", "HOME"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Grades", "GRADES"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Courses", "COURSES"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("My Registrations", "REGISTRATIONS"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Timetable", "TIMETABLE"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Fees", "FEES"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Hostel", "HOSTEL"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Reports", "REPORTS"));

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

    private JButton sideButton(String text, String cardKey) {
        JButton b = new JButton(text);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        b.setBackground(Color.WHITE);
        b.setForeground(TEXT_DARK);
        b.setFocusPainted(false);

        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC0C7CC)),
                new EmptyBorder(4, 10, 4, 10)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> {
            maintenanceBanner.setVisible(isMaintenanceOn());
            // Switch the card
            cardLayout.show(contentPanel, cardKey);
            switch (cardKey) {
                case "REGISTRATIONS" -> { if (regsPanel != null) regsPanel.reload(); }
                case "COURSES"       -> { if (coursesPanel != null) coursesPanel.reload(); }
                case "TIMETABLE"     -> { if (timetablePanel != null) timetablePanel.reload(); }
                case "HOME"          -> { if (homePanel != null) homePanel.reload(); }
                case "GRADES"        -> { if (gradesPanel != null) gradesPanel.reload(); }
            }
        });
        return b;
    }
    private JComponent buildHeader() {
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
        return header;
    }

    public JPanel createCard(String title, JComponent content) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        RoundedPanel tealBg = new RoundedPanel(24, TEAL);
        tealBg.setLayout(new BorderLayout());
        tealBg.setBorder(new EmptyBorder(10, 14, 14, 14));

        JLabel lbl = new JLabel(title);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setBorder(new EmptyBorder(0, 2, 6, 2));
        tealBg.add(lbl, BorderLayout.NORTH);

        JPanel innerWhite = new JPanel(new BorderLayout());
        innerWhite.setBackground(Color.WHITE);
        innerWhite.setBorder(new EmptyBorder(6, 6, 6, 6));
        innerWhite.add(content, BorderLayout.CENTER);

        tealBg.add(innerWhite, BorderLayout.CENTER);
        outer.add(tealBg, BorderLayout.CENTER);

        return outer;
    }

    public void reload() {
        coursesPanel.loadCatalogData();   // <-- correct
    }



    static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bg;

        RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class MiniCalendarPanel extends JPanel {
        private YearMonth currentMonth;
        private final LocalDate today;
        private final JLabel monthLabel;
        private final JPanel gridPanel;

        public MiniCalendarPanel() {
            setOpaque(false);
            setLayout(new BorderLayout(0, 8));

            today = LocalDate.now();
            currentMonth = YearMonth.from(today);

            monthLabel = new JLabel();
            monthLabel.setForeground(TEXT_DARK);
            monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            monthLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(monthLabel, BorderLayout.NORTH);

            gridPanel = new JPanel(new GridLayout(0, 7, 4, 4));
            gridPanel.setOpaque(false);
            add(gridPanel, BorderLayout.CENTER);

            buildCalendar();
        }

        private void buildCalendar() {
            gridPanel.removeAll();

            String monthName = currentMonth.getMonth().name().charAt(0)
                    + currentMonth.getMonth().name().substring(1).toLowerCase();

            monthLabel.setText(monthName + " " + currentMonth.getYear());

            for (DayOfWeek d : DayOfWeek.values()) {
                JLabel lbl = new JLabel(d.toString().substring(0, 3), SwingConstants.CENTER);
                lbl.setForeground(new Color(90, 90, 90));
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                gridPanel.add(lbl);
            }

            LocalDate first = currentMonth.atDay(1);
            int empty = first.getDayOfWeek().getValue();

            for (int i = 1; i < empty; i++) gridPanel.add(new JLabel(""));

            int days = currentMonth.lengthOfMonth();
            for (int day = 1; day <= days; day++) {
                LocalDate date = currentMonth.atDay(day);
                JLabel lbl = new JLabel(String.valueOf(day), SwingConstants.CENTER);

                lbl.setForeground(TEXT_DARK);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                if (date.equals(today)) {
                    lbl.setOpaque(true);
                    lbl.setBackground(new Color(0xFFE0B2));
                    lbl.setBorder(BorderFactory.createLineBorder(TEAL_DARK, 1));
                }
                gridPanel.add(lbl);
            }

            int totalCells = 7 * 7;
            while (gridPanel.getComponentCount() < totalCells)
                gridPanel.add(new JLabel(""));

            revalidate();
            repaint();
        }
    }

    private JPanel buildMaintenanceBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(255, 235, 130)); // yellow

        JLabel msg = new JLabel(
                " ⚠ ERP is in Maintenance Mode — You cannot register or drop courses.",
                SwingConstants.CENTER
        );
        msg.setForeground(Color.RED);
        msg.setFont(new Font("Segoe UI", Font.BOLD, 14));

        banner.add(msg, BorderLayout.CENTER);
        return banner;
    }


    public StudentProfile loadProfile() {
        String sql = "SELECT name, roll_no, program, year, email, phone FROM students WHERE user_id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new StudentProfile(
                        rs.getString("name"),
                        rs.getString("roll_no"),
                        rs.getString("program"),
                        rs.getInt("year"),
                        rs.getString("email"),
                        rs.getString("phone")
                );
            }

        } catch (Exception e) { e.printStackTrace(); }

        return null;
    }

    public record StudentProfile(
            String name,
            String roll,
            String program,
            int year,
            String email,
            String phone
    ) {}

    public java.util.List<String> loadTodaysClasses() {
        java.util.List<String> list = new java.util.ArrayList<>();

        String day = java.time.LocalDate.now().getDayOfWeek().name().substring(0, 3);

        String sql =
                "SELECT c.course_name, t.start_time, t.end_time " +
                        "FROM timetable t " +
                        "JOIN courses c ON t.course_id = c.course_id " +
                        "JOIN registrations r ON r.course_id = c.course_id " +
                        "WHERE r.user_id = ? AND t.day = ? " +
                        "ORDER BY t.start_time";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, day);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String line = rs.getString("course_name") +
                        " - " + rs.getString("start_time") +
                        " to " + rs.getString("end_time");
                list.add(line);
            }

        } catch (Exception e) { e.printStackTrace(); }

        return list;
    }

    public java.util.List<String> loadUpcomingDeadlines() {
        java.util.List<String> list = new java.util.ArrayList<>();

        String sql =
                "SELECT c.course_name, a.title, a.due_date " +
                        "FROM assignments a " +
                        "JOIN courses c ON a.course_id = c.course_id " +
                        "JOIN registrations r ON r.course_id = c.course_id " +
                        "WHERE r.user_id = ? AND a.due_date >= CURDATE() " +
                        "ORDER BY a.due_date ASC";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String line = rs.getString("course_name") +
                        " - " + rs.getString("title") +
                        " (Due: " + rs.getDate("due_date") + ")";
                list.add(line);
            }

        } catch (Exception e) { e.printStackTrace(); }

        return list;
    }

    private String loadUsername() {
        String sql = "SELECT username FROM users_auth WHERE user_id = ?";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");

        } catch (Exception e) { e.printStackTrace(); }

        return "Student";
    }

    private void loadAvailableCourses() {
        String query = "SELECT course_id, course_name, instructor FROM courses";

        try (Connection conn = DBConnection.getERPConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            DefaultTableModel model =
                    new DefaultTableModel(new String[]{"ID", "Course", "Instructor"}, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("course_id"),
                        rs.getString("course_name"),
                        rs.getString("instructor")
                });
            }

            tableCourses = new JTable(model);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadRegisteredCourses() {
        String sql =
                "SELECT r.course_id, c.course_name, c.instructor " +
                        "FROM registrations r " +
                        "JOIN courses c ON r.course_id = c.course_id " +
                        "WHERE r.user_id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            DefaultTableModel model =
                    new DefaultTableModel(new String[]{"ID", "Course", "Instructor"}, 0);

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("course_id"),
                        rs.getString("course_name"),
                        rs.getString("instructor")
                });
            }

            tableRegistered = new JTable(model);

        } catch (Exception e) { e.printStackTrace(); }
    }

    public record StudentInfo(String name, String rollNo, String program, int year) {}

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

    public String loadNextClass() {
        String sql = """
            SELECT c.course_name, s.day, s.start_time
            FROM timetable s
            JOIN courses c ON s.course_id = c.course_id
            JOIN registrations r ON r.course_id = s.course_id
            WHERE r.user_id = ?
            ORDER BY FIELD(s.day,'MON','TUE','WED','THU','FRI','SAT'), s.start_time
            LIMIT 1
            """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("course_name") + " — " +
                        rs.getString("day") + " @ " +
                        rs.getString("start_time");
            }

        } catch (Exception e) { e.printStackTrace(); }

        return "No upcoming classes.";
    }
}
