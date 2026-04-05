package erp.ui.instructor;

import erp.data.DBConnection;
import erp.ui.LoginFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class InstructorDashboard extends JFrame {

    private final int userId;
    private String username;

    // brand colors (same as StudentDashboard)
    public static final Color BG_WHITE = Color.WHITE;
    public static final Color SIDEBAR_BG = new Color(0xDDE3E6);
    public static final Color TEAL = new Color(0x00B3A4);
    public static final Color TEAL_DARK = new Color(0x009688);
    public static final Color TEXT_DARK = new Color(0x222222);

    private Image logoImage;
    private CardLayout cardLayout;
    private JPanel contentPanel;

    private JPanel homePanel;
    private InstructorContext context;
    private InstructorProfilePanel profilePanel;

    public InstructorDashboard(int userId) {
        this.userId = userId;
        this.username = loadUsername();
        this.context = new InstructorContext(userId, username);
        this.context.dashboard = this;


        setTitle("Instructor Dashboard");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            logoImage = ImageIO.read(getClass().getResource("/erp/ui/iiitd_logo.jpg"));
        } catch (Exception e) {
            System.out.println("InstructorDashboard: logo not found");
        }

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_WHITE);
        setContentPane(root);

        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(BG_WHITE);
        root.add(centerWrapper, BorderLayout.CENTER);

        centerWrapper.add(buildHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG_WHITE);
        centerWrapper.add(contentPanel, BorderLayout.CENTER);

        // HOME
        homePanel = buildHomePanel();
        contentPanel.add(homePanel, "HOME");

        // Placeholder pages
        contentPanel.add(new InstructorSectionsPanel(this, userId), "SECTIONS");


        contentPanel.add(new InstructorGradesPanel(this, userId), "GRADEBOOK");

        contentPanel.add(new InstructorStatsPanel(context), "STATS");

        profilePanel = new InstructorProfilePanel(userId);
        contentPanel.add(profilePanel, "PROFILE");


        cardLayout.show(contentPanel, "HOME");
        setVisible(true);
    }

    // ============================================================
    // SIDEBAR
    // ============================================================
    private JComponent buildSidebar() {
        JPanel side = new JPanel();
        side.setBackground(SIDEBAR_BG);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(160, 0));
        side.setBorder(new EmptyBorder(16, 10, 16, 10));

        side.add(sideButton("Home", "HOME"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("My Sections", "SECTIONS"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Gradebook", "GRADEBOOK"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Class Statistics", "STATS"));
        side.add(Box.createVerticalStrut(10));
        side.add(sideButton("Profile", "PROFILE"));

        side.add(Box.createVerticalGlue());

        JButton logout = new JButton("Logout");
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.setBackground(TEXT_DARK);
        logout.setForeground(Color.WHITE);
        logout.setFocusPainted(false);
        logout.setBorder(new EmptyBorder(6, 10, 6, 10));
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
            cardLayout.show(contentPanel, cardKey);
        });

        return b;
    }

    // ============================================================
    // HEADER
    // ============================================================
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_WHITE);
        header.setBorder(new EmptyBorder(8, 18, 8, 18));

        JPanel brand = new JPanel();
        brand.setBackground(BG_WHITE);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));

        JLabel logo = new JLabel();
        if (logoImage != null) {
            Image scaled = logoImage.getScaledInstance(450, 80, Image.SCALE_SMOOTH);
            logo.setIcon(new ImageIcon(scaled));
        } else {
            logo.setText("IIITD");
            logo.setFont(logo.getFont().deriveFont(Font.BOLD, 22f));
            logo.setForeground(TEXT_DARK);
        }

        brand.add(logo);
        header.add(brand, BorderLayout.WEST);

        return header;
    }

    private JPanel buildHomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_WHITE);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        panel.add(content, BorderLayout.CENTER);

        // Maintenance Mode
        if (isMaintenanceOn()) {
            JPanel banner = new JPanel(new BorderLayout());
            banner.setBackground(new Color(0xFFE082));
            banner.setBorder(new EmptyBorder(8, 12, 8, 12));

            JLabel label = new JLabel("Maintenance Mode is ON — view only.");
            label.setForeground(new Color(0x4E342E));
            label.setFont(new Font("Segoe UI", Font.BOLD, 13));

            banner.add(label, BorderLayout.CENTER);
            content.add(banner);
            content.add(Box.createVerticalStrut(16));
        }

        InstructorInfo info = loadInstructorInfo();
        InstructorStats stats = loadQuickStats();
        String term = computeCurrentTerm();
        List<String> courses = loadCoursesThisSem();
        List<Announcement> announcements = loadLatestAnnouncements();


        JPanel overviewInner = new JPanel();
        overviewInner.setOpaque(false);
        overviewInner.setLayout(new BoxLayout(overviewInner, BoxLayout.Y_AXIS));

        JLabel nameLbl = new JLabel("Welcome, " + info.name());
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        nameLbl.setForeground(TEXT_DARK);

        JLabel deptLbl = new JLabel("Department: " + info.department());
        deptLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        deptLbl.setForeground(TEXT_DARK);

        overviewInner.add(nameLbl);
        overviewInner.add(deptLbl);
        overviewInner.add(Box.createVerticalStrut(8));

        JLabel termLbl = new JLabel("Current Term: " + term);
        termLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel clist = new JPanel();
        clist.setOpaque(false);
        clist.setLayout(new BoxLayout(clist, BoxLayout.Y_AXIS));

        if (courses.isEmpty()) {
            clist.add(new JLabel("• None"));
        } else {
            for (String c : courses) {
                JLabel l = new JLabel("• " + c);
                l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                clist.add(l);
            }
        }

        overviewInner.add(termLbl);
        overviewInner.add(Box.createVerticalStrut(6));
        overviewInner.add(clist);

        JPanel statsInner = new JPanel(new GridLayout(1, 2, 12, 0));
        statsInner.setOpaque(false);
        statsInner.add(statBlock("Sections this term", String.valueOf(stats.numSections())));
        statsInner.add(statBlock("Students total", String.valueOf(stats.numStudents())));

        // LEFT column  (MATCHED EXACTLY TO STUDENT DASHBOARD)
        JPanel leftCol = new JPanel();
        leftCol.setOpaque(false);
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));

//
// 1. Instructor Overview  (like Student Profile)
//
        JPanel overviewCard = createCard("Instructor Overview", overviewInner);
        leftCol.add(overviewCard);
        leftCol.add(Box.createVerticalStrut(16));

//
// 2. Quick Stats (like Today's Classes)
//
        JPanel statsCard = createCard("Quick Stats", statsInner);
        leftCol.add(statsCard);
        leftCol.add(Box.createVerticalStrut(16));

//
// 3. Courses This Semester (like Summary)
//
        JPanel coursesCard = createCard("Courses This Semester", clist);
        leftCol.add(coursesCard);
        leftCol.add(Box.createVerticalStrut(16));
        leftCol.add(Box.createVerticalGlue());


        // RIGHT column
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setPreferredSize(new Dimension(420, 0));   // EXACT match with Student Dashboard
        rightCol.setMinimumSize(new Dimension(420, 0));

        // ANNOUNCEMENTS
        JPanel annInner = new JPanel();
        annInner.setOpaque(false);
        annInner.setLayout(new BoxLayout(annInner, BoxLayout.Y_AXIS));

        if (announcements.isEmpty()) {
            annInner.add(new JLabel("No announcements."));
        } else {
            for (Announcement a : announcements) {
                JPanel item = new JPanel();
                item.setOpaque(false);
                item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
                item.setBorder(new EmptyBorder(4, 0, 10, 0));

                JLabel t = new JLabel(a.title());
                t.setFont(new Font("Segoe UI", Font.BOLD, 13));
                JLabel m = new JLabel("<html>" + a.message() + "</html>");
                m.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                item.add(t);
                item.add(m);

                annInner.add(item);
            }
        }

        JPanel annCard = createCard("Announcements", annInner);
        rightCol.add(annCard);
        rightCol.add(Box.createVerticalStrut(16));

        // CALENDAR
        JPanel calCard = createCard("Calendar", new MiniCalendarPanel());
        rightCol.add(calCard);
        rightCol.add(Box.createVerticalGlue());
        calCard.setPreferredSize(new Dimension(420, 380));
        annCard.setPreferredSize(new Dimension(420, 230));

        // MAIN 2-column layout EXACTLY LIKE STUDENT DASHBOARD
        JPanel rowPanel = new JPanel(new BorderLayout(16, 0));
        rowPanel.setOpaque(false);

        rowPanel.add(leftCol, BorderLayout.CENTER);
        rowPanel.add(rightCol, BorderLayout.EAST);

        content.add(rowPanel);
        content.add(Box.createVerticalGlue());

        return panel;
    }

    private JComponent statBlock(String label, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 22));
        v.setForeground(TEXT_DARK);

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(new Color(90, 90, 90));

        p.add(v, BorderLayout.CENTER);
        p.add(l, BorderLayout.SOUTH);

        return p;
    }

    private JLabel centeredLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lbl.setForeground(TEXT_DARK);
        return lbl;
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
                gridPanel.add(new JLabel(""));

            revalidate();
            repaint();
        }
    }

    private String loadUsername() {
        String sql = "SELECT username FROM users_auth WHERE user_id = ?";

        try (Connection conn = DBConnection.getAuthConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("username");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Instructor";
    }

    public record InstructorInfo(String name, String department) {
    }

    private InstructorInfo loadInstructorInfo() {
        String sql = "SELECT name, department FROM instructors WHERE user_id = ?";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new InstructorInfo(
                        rs.getString("name"),
                        rs.getString("department")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new InstructorInfo("Instructor", "-");
    }

    public record InstructorStats(int numSections, int numStudents) {
    }

    public record Announcement(String title, String message) {
    }

    private List<Announcement> loadLatestAnnouncements() {
        List<Announcement> list = new ArrayList<>();

        String sql = """
        SELECT message, created_at
        FROM announcements
        ORDER BY created_at DESC
        LIMIT 5
    """;

        try (Connection conn = DBConnection.getERPConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Announcement(
                        "Announcement",                  // default title
                        rs.getString("message")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


    private InstructorStats loadQuickStats() {
        int sections = 0;
        int students = 0;

        String sqlSections =
                "SELECT COUNT(*) AS cnt FROM sections WHERE instructor_id = ?";

        String sqlStudents =
                "SELECT COUNT(DISTINCT e.student_id) AS cnt " +
                        "FROM enrollments e " +
                        "JOIN sections s ON e.section_id = s.section_id " +
                        "WHERE s.instructor_id = ?";

        try (Connection conn = DBConnection.getERPConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(sqlSections)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) sections = rs.getInt("cnt");
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlStudents)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) students = rs.getInt("cnt");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new InstructorStats(sections, students);
    }

    private List<String> loadCoursesThisSem() {
        List<String> list = new ArrayList<>();

        String sql =
                "SELECT DISTINCT c.course_name " +
                        "FROM courses c " +
                        "JOIN sections s ON s.course_id = c.course_id " +
                        "WHERE s.instructor_id = ?" +
                        "ORDER BY c.course_name";

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) list.add(rs.getString("course_name"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public boolean isMaintenanceOn() {
        String sql = "SELECT value FROM settings WHERE `key`='maintenance_mode'";
        try (Connection conn = DBConnection.getERPConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getString("value").equalsIgnoreCase("ON");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String computeCurrentTerm() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        String sem;
        if (month >= 1 && month <= 5) sem = "Winter";
        else if (month >= 6 && month <= 7) sem = "Summer";
        else sem = "Monsoon";

        return sem + " " + year;
    }
}
