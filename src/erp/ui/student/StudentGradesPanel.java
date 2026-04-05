package erp.ui.student;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentGradesPanel extends JPanel {

    private final StudentDashboard dashboard;
    private final int userId;

    private JTable summaryTable;
    private JTable componentsTable;

    private DefaultTableModel summaryModel;
    private DefaultTableModel componentsModel;

    private JLabel cgpaLabel;
    private JLabel creditsLabel;

    public StudentGradesPanel(StudentDashboard dashboard, int userId) {
        this.dashboard = dashboard;
        this.userId = userId;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel wrapper = new RoundedPanel(24, new Color(0x00B3A4));
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(14, 14, 14, 14));

        wrapper.add(buildTopBar(), BorderLayout.NORTH);
        wrapper.add(buildInnerSplit(), BorderLayout.CENTER);
        wrapper.add(buildBottomBar(), BorderLayout.SOUTH);

        add(wrapper, BorderLayout.CENTER);

        loadSummary();
        loadCgpaSummary();
    }

    // ============================================================
    private JComponent buildTopBar() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Grades");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(6));

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        stats.setOpaque(false);

        cgpaLabel = new JLabel("CGPA: N/A");
        cgpaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cgpaLabel.setForeground(Color.WHITE);

        creditsLabel = new JLabel("Completed Credits: 0");
        creditsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        creditsLabel.setForeground(Color.WHITE);

        stats.add(cgpaLabel);
        stats.add(new JLabel("|"));
        stats.add(creditsLabel);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        top.add(stats);

        if (dashboard.isMaintenanceOn()) {
            JPanel banner = new JPanel(new BorderLayout());
            banner.setBackground(new Color(255, 230, 160));
            banner.setBorder(new EmptyBorder(6, 10, 6, 10));
            banner.add(new JLabel("Maintenance Mode: You can view but not modify."),
                    BorderLayout.WEST);
            banner.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(Box.createVerticalStrut(8));
            top.add(banner);
        }

        return top;
    }

    // ============================================================
    private JComponent buildInnerSplit() {

        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Color.WHITE);
        inner.setBorder(new EmptyBorder(10, 10, 10, 10));

        summaryModel = new DefaultTableModel(
                new String[]{
                        "Enrollment ID", "Course Code", "Course Title",
                        "Section", "Semester", "Year", "Letter Grade"
                }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        summaryTable = new JTable(summaryModel);
        summaryTable.setRowHeight(22);

        JScrollPane summaryScroll = new JScrollPane(summaryTable);
        summaryScroll.getViewport().setBackground(Color.WHITE);
        summaryScroll.setBorder(null);

        summaryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadComponents();
        });

        componentsModel = new DefaultTableModel(
                new String[]{"Component", "Score", "Final Grade (If Present)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        componentsTable = new JTable(componentsModel);
        componentsTable.setRowHeight(22);

        JScrollPane compScroll = new JScrollPane(componentsTable);
        compScroll.getViewport().setBackground(Color.WHITE);
        compScroll.setBorder(null);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, summaryScroll, compScroll);
        split.setResizeWeight(0.55);
        split.setBorder(null);

        inner.add(split, BorderLayout.CENTER);

        return inner;
    }

    // ============================================================
    private JComponent buildBottomBar() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        // --- Refresh Button ---
        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.setBackground(new Color(0x00B3A4));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnRefresh.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> reload());

        // --- CSV Export Button ---
        JButton btnCSV = new JButton("Download Transcript (CSV)");
        btnCSV.setBackground(new Color(0x00B3A4));
        btnCSV.setForeground(Color.WHITE);
        btnCSV.setFocusPainted(false);
        btnCSV.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCSV.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btnCSV.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCSV.addActionListener(e -> exportTranscriptCSV());

        bottom.add(btnRefresh);
        bottom.add(btnCSV);

        return bottom;
    }

    // ============================================================
    public void exportTranscriptCSV() {
        if (summaryModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No grades available to export.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("transcript.csv"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = chooser.getSelectedFile();

        try (FileWriter writer = new FileWriter(file)) {

            writer.write("Course Code,Course Name,Letter Grade\n");

            for (int i = 0; i < summaryModel.getRowCount(); i++) {
                writer.write(
                        summaryModel.getValueAt(i, 1) + "," +
                                "\"" + summaryModel.getValueAt(i, 2) + "\"," +
                                summaryModel.getValueAt(i, 6) +
                                "\n"
                );
            }

            JOptionPane.showMessageDialog(this,
                    "Transcript saved to:\n" + file.getAbsolutePath());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save CSV: " + e.getMessage());
        }
    }

    // ============================================================
    private void loadSummary() {
        summaryModel.setRowCount(0);

        String sql = """
            SELECT
                r.reg_id AS enrollment_id,
                c.course_id,
                c.code,
                c.title,
                CONCAT('SEC-', COALESCE(tt.section_id, 1)) AS section_label,
                tt.semester,
                tt.year,
                fg.letter_grade
            FROM registrations r
            JOIN courses c ON r.course_id = c.course_id

            LEFT JOIN (
                SELECT 
                    course_id,
                    MIN(id) AS section_id,
                    MIN(semester) AS semester,
                    MIN(year) AS year
                FROM timetable
                GROUP BY course_id
            ) tt ON tt.course_id = c.course_id

            LEFT JOIN (
                SELECT user_id, course_id, MAX(fg_id) AS fg_id
                FROM final_grades
                GROUP BY user_id, course_id
            ) map_fg 
                ON map_fg.user_id = r.user_id
               AND map_fg.course_id = c.course_id

            LEFT JOIN final_grades fg ON fg.fg_id = map_fg.fg_id

            WHERE r.user_id = ?
            ORDER BY c.code
            """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String letter = rs.getString("letter_grade");
                String displayGrade = (letter != null && !letter.isBlank()) ? letter : "Pending";

                summaryModel.addRow(new Object[]{
                        rs.getInt("enrollment_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("section_label"),
                        rs.getString("semester"),
                        rs.getInt("year"),
                        displayGrade
                });
            }

        } catch (Exception e) { e.printStackTrace(); }

        if (summaryModel.getRowCount() > 0) {
            summaryTable.setRowSelectionInterval(0, 0);
            loadComponents();
        } else {
            componentsModel.setRowCount(0);
        }
    }

    // ============================================================
    private void loadComponents() {
        componentsModel.setRowCount(0);

        int row = summaryTable.getSelectedRow();
        if (row < 0) return;

        int modelRow = summaryTable.convertRowIndexToModel(row);
        int enrollmentId = (Integer) summaryModel.getValueAt(modelRow, 0);

        String sql = """
            SELECT component, score, max_score, final_grade
            FROM grades
            WHERE enrollment_id = ?
            ORDER BY component
            """;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, enrollmentId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                String component = rs.getString("component");
                var score = rs.getBigDecimal("score");
                var max = rs.getBigDecimal("max_score");
                var finalGrade = rs.getBigDecimal("final_grade");

                String scoreDisplay;
                if (score != null && max != null) {
                    scoreDisplay =
                            score.stripTrailingZeros().toPlainString() + "/" +
                                    max.stripTrailingZeros().toPlainString();
                } else {
                    scoreDisplay = (score != null)
                            ? score.stripTrailingZeros().toPlainString()
                            : "N/A";
                }

                componentsModel.addRow(new Object[]{
                        component,
                        scoreDisplay,
                        finalGrade
                });
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    // ============================================================
    private void loadCgpaSummary() {
        String sql = """
            SELECT fg.letter_grade, c.credits
            FROM final_grades fg
            JOIN (
                SELECT user_id, course_id, MAX(fg_id) AS fg_id
                FROM final_grades
                GROUP BY user_id, course_id
            ) map_fg ON map_fg.fg_id = fg.fg_id
            JOIN courses c ON c.course_id = fg.course_id
            WHERE fg.user_id = ?
            """;

        double totalPoints = 0.0;
        int totalCredits = 0;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String lg = rs.getString("letter_grade");
                int cr = rs.getInt("credits");

                if (lg == null || lg.isBlank()) continue;

                double pts = letterToPoints(lg);
                totalPoints += pts * cr;
                totalCredits += cr;
            }

        } catch (Exception e) { e.printStackTrace(); }

        if (totalCredits == 0) {
            cgpaLabel.setText("CGPA: N/A");
            creditsLabel.setText("Completed Credits: 0");
        } else {
            double cgpa = totalPoints / totalCredits;
            cgpaLabel.setText(String.format("CGPA: %.2f", cgpa));
            creditsLabel.setText("Completed Credits: " + totalCredits);
        }
    }

    // ============================================================
    private double letterToPoints(String lg) {
        if (lg == null) return 0.0;
        lg = lg.trim().toUpperCase();

        switch (lg) {
            case "A+":
            case "A":  return 10.0;
            case "A-": return 9.0;
            case "B+": return 8.5;
            case "B":  return 8.0;
            case "B-": return 7.0;
            case "C":  return 6.0;
            case "D":  return 5.0;
            default:   return 0.0;
        }
    }

    // ============================================================
    public void reload() {
        loadSummary();
        loadCgpaSummary();
    }

    // ============================================================
    static class RoundedPanel extends JPanel {
        int arc;
        Color bg;
        RoundedPanel(int arc, Color bg) { this.arc = arc; this.bg = bg; setOpaque(false); }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            super.paintComponent(g);
        }
    }
}
