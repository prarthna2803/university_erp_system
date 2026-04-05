package erp.ui.student;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class StudentTimetablePanel extends JPanel {

    private final StudentDashboard dashboard;
    private final int userId;

    private Map<String, java.util.List<ClassSlot>> timetableMap;
    private LocalTime nextClassTime;
    private String nextClassDay;

    private static final String[] DAYS = {"MON", "TUE", "WED", "THU", "FRI"};

    public StudentTimetablePanel(StudentDashboard dashboard, int userId) {
        this.dashboard = dashboard;
        this.userId = userId;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 18, 18, 18));

        // ===== Outer cyan rounded wrapper =====
        JPanel wrapper = new RoundedPanel(26, new Color(0x00B3A4));
        wrapper.setOpaque(false);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(16, 16, 16, 16));

        wrapper.add(buildTopBar(), BorderLayout.NORTH);

        // Load & Build timetable
        loadTimetableFromDB();
        wrapper.add(buildInnerTable(), BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    // ================= TOP BAR =================
    private JComponent buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 4, 12, 4));

        JLabel title = new JLabel("My Timetable");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        top.add(title, BorderLayout.WEST);
        return top;
    }

    // =============== INNER WHITE ROUNDED PANEL ===============
    private JComponent buildInnerTable() {
        JPanel inner = new RoundedPanel(18, Color.WHITE);
        inner.setOpaque(false);
        inner.setLayout(new BorderLayout());
        inner.setBorder(new EmptyBorder(20, 20, 20, 20));

        inner.add(buildGridView(), BorderLayout.CENTER);
        return new JScrollPane(inner);
    }

    // =================== LOAD TIMETABLE ======================
    private void loadTimetableFromDB() {
        timetableMap = new HashMap<>();
        for (String d : DAYS) timetableMap.put(d, new ArrayList<>());

        nextClassDay = null;
        nextClassTime = null;

        String sql = """
            SELECT 
                t.day,
                t.start_time,
                t.end_time,
                c.code,
                c.title,
                t.id AS section_id,
                t.room,
                c.instructor,
                t.semester,
                t.year
            FROM registrations r
            JOIN courses c ON r.course_id = c.course_id
            JOIN timetable t ON t.course_id = c.course_id
            WHERE r.user_id = ?
            ORDER BY FIELD(t.day,'MON','TUE','WED','THU','FRI'), t.start_time
            """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            String today = LocalDate.now().getDayOfWeek().name().substring(0, 3);

            while (rs.next()) {
                String day = rs.getString("day");
                if (!timetableMap.containsKey(day)) continue;

                LocalTime start = rs.getTime("start_time").toLocalTime();
                LocalTime end   = rs.getTime("end_time").toLocalTime();

                ClassSlot slot = new ClassSlot(
                        day, start, end,
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("section_id"),
                        rs.getString("room"),
                        rs.getString("instructor"),
                        rs.getString("semester"),
                        rs.getInt("year")
                );

                timetableMap.get(day).add(slot);

                // Determine next class
                if (day.equals(today) && start.isAfter(LocalTime.now())) {
                    if (nextClassTime == null || start.isBefore(nextClassTime)) {
                        nextClassDay = day;
                        nextClassTime = start;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==================== GRID VIEW =========================
    private JPanel buildGridView() {

        // HOURS: 8 to 19 = 12 rows
        int rows = 13; // header + 12 hours
        int cols = 6;  // time column + 5 weekdays

        JPanel grid = new JPanel(new GridLayout(rows, cols));
        grid.setOpaque(false);

        // CELL SIZE
        int cellWidth = 140;
        int cellHeight = 70;

        // -------- HEADER ROW --------
        grid.add(makeHeaderCell("")); // top-left empty

        String today = LocalDate.now().getDayOfWeek().name().substring(0, 3);

        for (String day : DAYS) {
            JLabel h = new JLabel(day, SwingConstants.CENTER);
            h.setOpaque(true);
            h.setFont(new Font("Segoe UI", Font.BOLD, 14));
            h.setPreferredSize(new Dimension(cellWidth, cellHeight));
            h.setBackground(day.equals(today) ? new Color(255,230,150) : new Color(235,235,235));
            h.setBorder(BorderFactory.createLineBorder(new Color(200,200,200)));
            grid.add(h);
        }

        // -------- BODY CELLS --------
        for (int hour = 8; hour <= 19; hour++) {

            // Left time label
            JLabel time = new JLabel(hour + ":00", SwingConstants.CENTER);
            time.setOpaque(true);
            time.setFont(new Font("Segoe UI", Font.BOLD, 12));
            time.setBackground(new Color(245,245,245));
            time.setPreferredSize(new Dimension(cellWidth, cellHeight));
            time.setBorder(BorderFactory.createLineBorder(new Color(210,210,210)));
            grid.add(time);

            // Day columns
            for (String day : DAYS) {
                JPanel cell = new RoundedPanel(16, new Color(255,255,255));
                cell.setLayout(new BorderLayout());
                cell.setPreferredSize(new Dimension(cellWidth, cellHeight));
                cell.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));

                ClassSlot cls = getClassAtHour(day, hour);

                if (cls != null) {
                    cell.setBackground(new Color(180,220,255));

                    JTextArea a = new JTextArea(
                            cls.code + " (" + cls.title + ")\n" +
                                    cls.start + "–" + cls.end + "\n" +
                                    "Room: " + cls.room
                    );
                    a.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    a.setOpaque(false);
                    a.setEditable(false);
                    cell.add(a, BorderLayout.CENTER);

                    // Highlight next class
                    if (cls.day.equals(nextClassDay) && cls.start.equals(nextClassTime)) {
                        cell.setBackground(new Color(255,210,140));
                    }
                }

                grid.add(cell);
            }
        }

        return grid;
    }

    private JComponent makeHeaderCell(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setOpaque(true);
        l.setBackground(new Color(240,240,240));
        l.setBorder(BorderFactory.createLineBorder(new Color(200,200,200)));
        return l;
    }


    private ClassSlot getClassAtHour(String day, int hour) {
        for (ClassSlot cls : timetableMap.get(day)) {
            if (cls.start.getHour() <= hour && cls.end.getHour() > hour)
                return cls;
        }
        return null;
    }

    // ===================== RELOAD SUPPORT =======================
    public void reload() {
        loadTimetableFromDB();
        removeAll();
        setLayout(new BorderLayout());

        JPanel wrapper = new RoundedPanel(26, new Color(0x00B3A4));
        wrapper.setOpaque(false);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(16, 16, 16, 16));

        wrapper.add(buildTopBar(), BorderLayout.NORTH);
        wrapper.add(buildInnerTable(), BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // ===================== Class Structure ======================
    private static class ClassSlot {
        String day, code, title, room, instructor, sem;
        LocalTime start, end;
        int sectionId, year;

        ClassSlot(String day, LocalTime start, LocalTime end,
                  String code, String title, int sectionId,
                  String room, String instructor, String sem, int year) {
            this.day = day;
            this.start = start;
            this.end = end;
            this.code = code;
            this.title = title;
            this.sectionId = sectionId;
            this.room = room;
            this.instructor = instructor;
            this.sem = sem;
            this.year = year;
        }
    }

    // ===================== Rounded Panel =====================
    static class RoundedPanel extends JPanel {
        int arc;
        Color bg;

        RoundedPanel(int arc, Color bg) {
            this.arc = arc;
            this.bg = bg;
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
        }
    }
}
