package erp.ui.student;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class StudentHomePanel extends JPanel {

    public StudentHomePanel(StudentDashboard dashboard) {

        setLayout(new BorderLayout());
        setBackground(StudentDashboard.BG_WHITE);
        // outer margin around the whole home panel
        setBorder(new EmptyBorder(18, 18, 18, 18));

        // ===== RIGHT COLUMN (Announcements + Calendar) =====
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setPreferredSize(new Dimension(420, 0));
        // add some padding on the left so it doesn't stick to the center
        rightCol.setBorder(new EmptyBorder(0, 12, 0, 0));

        // Announcements
        JTextArea annText = new JTextArea(
                "No announcements yet.\n\nYour instructors will post announcements here.");
        annText.setLineWrap(true);
        annText.setWrapStyleWord(true);
        annText.setEditable(false);
        annText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        annText.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane annScroll = new JScrollPane(annText);
        annScroll.setBorder(null);
        annScroll.getViewport().setBackground(Color.WHITE);

        rightCol.add(dashboard.createCard("Announcements", annScroll));
        rightCol.add(Box.createVerticalStrut(18));

        // Calendar
        StudentDashboard.MiniCalendarPanel calPanel = new StudentDashboard.MiniCalendarPanel();
        calPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        rightCol.add(dashboard.createCard("Calendar", calPanel));

        add(rightCol, BorderLayout.EAST);

        // ===== LEFT MAIN AREA =====
        JPanel leftMain = new JPanel();
        leftMain.setOpaque(false);
        leftMain.setLayout(new BoxLayout(leftMain, BoxLayout.Y_AXIS));
        // add padding on the right so it doesn't touch the right column
        leftMain.setBorder(new EmptyBorder(0, 0, 0, 12));
        add(leftMain, BorderLayout.CENTER);

        // ----- Student Profile -----
        StudentDashboard.StudentProfile p = dashboard.loadProfile();

        JPanel prof = new JPanel(new GridLayout(0, 1, 4, 4));
        prof.setOpaque(false);
        prof.add(new JLabel("Name: " + p.name()));
        prof.add(new JLabel("Roll No: " + p.roll()));
        prof.add(new JLabel("Program: " + p.program()));
        prof.add(new JLabel("Year: " + p.year()));
        if (p.email() != null) prof.add(new JLabel("Email: " + p.email()));
        if (p.phone() != null) prof.add(new JLabel("Phone: " + p.phone()));

        leftMain.add(dashboard.createCard("Student Profile", prof));
        leftMain.add(Box.createVerticalStrut(18));

        // ----- Maintenance Banner -----
        if (dashboard.isMaintenanceOn()) {
            JPanel banner = new JPanel();
            banner.setBackground(new Color(255, 230, 150));
            banner.setBorder(new EmptyBorder(10, 10, 10, 10));
            banner.add(new JLabel("Maintenance Mode: You can view but cannot make changes."));
            leftMain.add(banner);
            leftMain.add(Box.createVerticalStrut(18));
        }

        // ----- Today's Classes -----
        List<String> todays = dashboard.loadTodaysClasses();

        JPanel todayBox = new JPanel();
        todayBox.setLayout(new BoxLayout(todayBox, BoxLayout.Y_AXIS));
        todayBox.setOpaque(false);

        if (todays.isEmpty()) {
            todayBox.add(new JLabel("No classes today."));
        } else {
            for (String line : todays) {
                JLabel lbl = new JLabel("• " + line);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                todayBox.add(lbl);
            }
        }

        leftMain.add(dashboard.createCard("Today's Classes", todayBox));
        leftMain.add(Box.createVerticalStrut(18));

        // ----- Summary Stats -----
        int registeredCount = dashboard.tableRegistered != null
                ? dashboard.tableRegistered.getRowCount()
                : 0;

        int totalCredits = 0;
        if (dashboard.tableRegistered != null) {
            for (int i = 0; i < dashboard.tableRegistered.getRowCount(); i++) {
                Object val = dashboard.tableRegistered.getValueAt(i, 0);
                if (val instanceof Integer cid) {
                    totalCredits += fetchCredits(cid);
                } else if (val != null) {
                    try {
                        int cid = Integer.parseInt(val.toString());
                        totalCredits += fetchCredits(cid);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        int assignmentsDue = dashboard.loadUpcomingDeadlines().size();

        JPanel statsPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        statsPanel.setOpaque(false);
        statsPanel.add(new JLabel("Registered Courses: " + registeredCount));
        statsPanel.add(new JLabel("Total Credits: " + totalCredits));
        statsPanel.add(new JLabel("Upcoming Assignments: " + assignmentsDue));

        leftMain.add(dashboard.createCard("Summary", statsPanel));
        leftMain.add(Box.createVerticalStrut(18));

        // ----- Upcoming Deadlines -----
        List<String> deadlines = dashboard.loadUpcomingDeadlines();
        JPanel dlPanel = new JPanel();
        dlPanel.setLayout(new BoxLayout(dlPanel, BoxLayout.Y_AXIS));
        dlPanel.setOpaque(false);

        if (deadlines.isEmpty()) {
            dlPanel.add(new JLabel("No upcoming deadlines."));
        } else {
            for (String d : deadlines) {
                JLabel lbl = new JLabel("• " + d);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                dlPanel.add(lbl);
            }
        }

        leftMain.add(dashboard.createCard("Upcoming Deadlines", dlPanel));
    }

    // Helper to fetch course credits from DB
    private int fetchCredits(int courseId) {
        String sql = "SELECT credits FROM courses WHERE course_id = ?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("credits");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    public void reload() {
        revalidate();
        repaint();
    }
}
