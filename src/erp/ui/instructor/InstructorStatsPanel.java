package erp.ui.instructor;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.text.DecimalFormat;

public class InstructorStatsPanel extends JPanel {

    private final InstructorContext context;
    private final int instructorId;
    private final DefaultTableModel model;
    private final JTable table;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public InstructorStatsPanel(InstructorContext context) {
        this.context = context;
        this.instructorId = context.userId; // matches usage in other panels

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // teal rounded outer (matches dashboard cards)
        InstructorDashboard.RoundedPanel tealBg =
                new InstructorDashboard.RoundedPanel(24, InstructorDashboard.TEAL);
        tealBg.setLayout(new BorderLayout());
        tealBg.setBorder(new EmptyBorder(12, 14, 14, 14));
        add(tealBg, BorderLayout.CENTER);

        JLabel title = new JLabel("Class Statistics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        tealBg.add(title, BorderLayout.NORTH);

        // white inner panel
        JPanel innerWhite = new JPanel(new BorderLayout());
        innerWhite.setBackground(Color.WHITE);
        innerWhite.setBorder(new EmptyBorder(16, 16, 16, 16));
        tealBg.add(innerWhite, BorderLayout.CENTER);

        // hint + controls row
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel hint = new JLabel("Per-course averages (uses final_grades if present, otherwise computes weighted score)");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(new Color(0x5A6A85));
        top.add(hint, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        controls.setOpaque(false);
        JButton refresh = new JButton("Refresh");
        styleButton(refresh);
        refresh.addActionListener(e -> loadStats());
        controls.add(refresh);
        top.add(controls, BorderLayout.EAST);

        innerWhite.add(top, BorderLayout.NORTH);

        // table
        String[] cols = {"Course", "Avg (%)", "Highest (%)", "Lowest (%)", "Students"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setReorderingAllowed(false);
        table.setForeground(InstructorDashboard.TEXT_DARK);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(8, 8, 8, 8));
        innerWhite.add(sp, BorderLayout.CENTER);

        // initial load
        loadStats();
    }

    private void styleButton(JButton b) {
        b.setBackground(InstructorDashboard.TEAL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void loadStats() {
        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            model.addRow(new Object[]{"Loading...", "", "", "", ""});
            table.setEnabled(false);
        });

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                String coursesSql = """
                    SELECT DISTINCT s.course_id, c.title
                    FROM sections s
                    JOIN courses c ON c.course_id = s.course_id
                    WHERE s.instructor_id = ?
                    ORDER BY c.title
                    """;

                String aggSql = """
                    SELECT
                      ROUND(AVG(final_numeric),2) AS avg_score,
                      MAX(final_numeric) AS max_score,
                      MIN(final_numeric) AS min_score,
                      COUNT(*) AS students
                    FROM (
                      SELECT r.reg_id,
                        COALESCE(fg.numeric_grade,
                          ROUND(SUM( (COALESCE(g.score,0) / NULLIF(gc.max_score,0)) * COALESCE(gc.weight_percent,0) ),2)
                        ) AS final_numeric
                      FROM registrations r
                      LEFT JOIN final_grades fg ON fg.user_id = r.user_id AND fg.course_id = r.course_id
                      LEFT JOIN grades g ON g.enrollment_id = r.reg_id
                      LEFT JOIN grading_components gc ON gc.course_id = r.course_id AND gc.name = g.component
                      WHERE r.course_id = ?
                      GROUP BY r.reg_id, fg.numeric_grade
                    ) t
                    """;

                try (Connection conn = DBConnection.getERPConnection();
                     PreparedStatement pcs = conn.prepareStatement(coursesSql);
                     PreparedStatement pas = conn.prepareStatement(aggSql)) {

                    pcs.setInt(1, instructorId);
                    try (ResultSet rcs = pcs.executeQuery()) {
                        boolean anyCourse = false;
                        SwingUtilities.invokeLater(() -> model.setRowCount(0));

                        while (rcs.next()) {
                            anyCourse = true;
                            int courseId = rcs.getInt("course_id");
                            String title = rcs.getString("title");

                            pas.setInt(1, courseId);
                            try (ResultSet ars = pas.executeQuery()) {
                                if (ars.next()) {
                                    Object avgObj = ars.getObject("avg_score");
                                    Object maxObj = ars.getObject("max_score");
                                    Object minObj = ars.getObject("min_score");
                                    int students = ars.getInt("students");

                                    String avgStr = avgObj == null ? "—" : formatNumber(avgObj);
                                    String maxStr = maxObj == null ? "—" : formatNumber(maxObj);
                                    String minStr = minObj == null ? "—" : formatNumber(minObj);

                                    SwingUtilities.invokeLater(() -> model.addRow(new Object[]{
                                            title, avgStr, maxStr, minStr, students
                                    }));
                                } else {
                                    SwingUtilities.invokeLater(() -> model.addRow(new Object[]{
                                            title, "—", "—", "—", 0
                                    }));
                                }
                            } catch (SQLException exAgg) {
                                SwingUtilities.invokeLater(() -> model.addRow(new Object[]{
                                        title, "Err", "Err", "Err", 0
                                }));
                                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(InstructorStatsPanel.this,
                                        "Unable to compute statistics for course: " + title + ".\nDetails: " + exAgg.getMessage(),
                                        "Partial Data Error", JOptionPane.WARNING_MESSAGE));
                            }
                        }

                        if (!anyCourse) {
                            SwingUtilities.invokeLater(() -> model.addRow(new Object[]{"No courses found for instructor", "", "", "", 0}));
                        }
                    }

                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        model.addRow(new Object[]{"Error loading statistics", "", "", "", 0});
                        JOptionPane.showMessageDialog(InstructorStatsPanel.this,
                                "Unable to load statistics from the database.\nPlease check your connection or contact the system administrator.\n\nDetails: " + ex.getMessage(),
                                "Database Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        model.addRow(new Object[]{"Error loading statistics", "", "", "", 0});
                        JOptionPane.showMessageDialog(InstructorStatsPanel.this,
                                "An unexpected error occurred while loading statistics.\n\nDetails: " + ex.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> table.setEnabled(true));
            }
        }.execute();
    }

    private String formatNumber(Object val) {
        if (val == null) return "—";
        try {
            if (val instanceof Number) return df.format(((Number) val).doubleValue());
            return df.format(new BigDecimal(String.valueOf(val)).doubleValue());
        } catch (Exception e) {
            return String.valueOf(val);
        }
    }
}
