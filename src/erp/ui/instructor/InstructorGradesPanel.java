package erp.ui.instructor;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;
import java.util.List;

public class InstructorGradesPanel extends JPanel {

    private final InstructorDashboard dashboard;
    private final int instructorId;

    // UI
    private JComboBox<SectionItem> sectionCombo;
    private DefaultTableModel componentsModel;
    private JTable componentsTable;

    private DefaultTableModel studentsModel;
    private JTable studentsTable;

    private JLabel status;

    // internal state for current selection
    private int currentSectionId = -1;
    private int currentCourseId = -1;
    private String currentSemester = null;
    private List<ComponentDef> currentComponents = new ArrayList<>(); // ordered

    public InstructorGradesPanel(InstructorContext context, int sectionId, String courseName) {
        // use the main constructor that Gradebook already uses
        this(context.dashboard, context.userId);

        // load the selected section after UI builds
        SwingUtilities.invokeLater(() -> {
            try {
                // courseName looks like "CS101 - Data Structures"
                String courseCode = courseName.split(" - ")[0].trim();
                int courseId = findCourseIdByCode(courseCode);

                // semester isn’t available here, so pass empty string
                loadForSection(sectionId, courseId, "");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    // helper
    private int findCourseIdByCode(String code) {
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT course_id FROM courses WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public InstructorGradesPanel(InstructorDashboard dashboard, int instructorId) {
        this.dashboard = dashboard;
        this.instructorId = instructorId;

        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel wrapper = new InstructorDashboard.RoundedPanel(24, InstructorDashboard.TEAL);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        // allow wrapper to fill available space horizontally so internal header can center visually
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(12, 12, 12, 12);
        add(wrapper, gbc);

        wrapper.add(buildTop(), BorderLayout.NORTH);
        wrapper.add(buildCenter(), BorderLayout.CENTER);
        wrapper.add(buildBottom(), BorderLayout.SOUTH);

        // ensure grading_components exists (create if missing)
        ensureGradingComponentsTable();

        loadSectionsForInstructor();
    }

    // ---------------- UI builders ----------------
    private JComponent buildTop() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Gradebook");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        // center the title inside the BoxLayout
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(8));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.setOpaque(false);

        row.add(new JLabel("Section:"));

        sectionCombo = new JComboBox<>();
        sectionCombo.setPreferredSize(new Dimension(420, 28));
        sectionCombo.addActionListener(e -> {
            SectionItem si = (SectionItem) sectionCombo.getSelectedItem();
            if (si != null) loadForSection(si.sectionId, si.courseId, si.semester);
        });

        row.add(sectionCombo);

        JButton btnEnsure = new JButton("Ensure Registrations");
        styleButton(btnEnsure);
        btnEnsure.addActionListener(e -> {
            SectionItem si = (SectionItem) sectionCombo.getSelectedItem();
            if (si == null) return;
            new SwingWorker<Integer, Void>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    return ensureRegistrationsForSection(si.sectionId);
                }

                @Override
                protected void done() {
                    try {
                        int created = get();
                        JOptionPane.showMessageDialog(InstructorGradesPanel.this,
                                "Created " + created + " missing registrations (if any).");
                        loadForSection(si.sectionId, si.courseId, si.semester);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(InstructorGradesPanel.this, "Error: " + ex.getMessage());
                    }
                }
            }.execute();
        });
        row.add(btnEnsure);

        top.add(row);
        top.add(Box.createVerticalStrut(6));

        status = new JLabel();
        status.setForeground(Color.WHITE);

        return top;
    }

    private JComponent buildCenter() {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.35);
        split.setBorder(null);
        split.setOpaque(false);

        // components panel
        JPanel compCard = new JPanel(new BorderLayout());
        compCard.setOpaque(false);
        compCard.setBorder(new EmptyBorder(8, 8, 8, 8));

        componentsModel = new DefaultTableModel(new String[]{
                "ID", "Component", "Max Score", "Weight (%)"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                // allow editing except ID col
                return c != 0;
            }
        };
        componentsTable = new JTable(componentsModel);
        componentsTable.setRowHeight(22);
        componentsTable.removeColumn(componentsTable.getColumnModel().getColumn(0)); // hide ID column

        JScrollPane compScroll = new JScrollPane(componentsTable);
        compScroll.setBorder(null);
        compCard.add(compScroll, BorderLayout.CENTER);

        JPanel compBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        compBtns.setOpaque(false);
        // per latest request: no Add/Delete buttons; keep Edit Component and Save Components
        JButton editComp = new JButton("Edit Component");
        styleButton(editComp);
        JButton saveComp = new JButton("Save Components");
        styleButton(saveComp);

        editComp.addActionListener(e -> {
            int r = componentsTable.getSelectedRow();
            if (r >= 0) {
                int mr = componentsTable.convertRowIndexToModel(r);
                showEditComponentDialog(mr);
                // editing names/weights/max should reflect immediately in students header (name change)
                rebuildStudentsModelWithCurrentComponents(currentComponents.size());
            } else {
                JOptionPane.showMessageDialog(this, "Select a component row to edit.");
            }
        });

        saveComp.addActionListener(e -> saveComponents());

        compBtns.add(editComp);
        compBtns.add(saveComp);

        compCard.add(compBtns, BorderLayout.SOUTH);

        split.setTopComponent(createCard("Components (define assessment parts)", compCard));

        // students / scores panel
        JPanel stuCard = new JPanel(new BorderLayout());
        stuCard.setOpaque(false);
        stuCard.setBorder(new EmptyBorder(8, 8, 8, 8));

        studentsModel = new DefaultTableModel(); // columns set dynamically
        studentsTable = new JTable(studentsModel);
        studentsTable.setRowHeight(22);

        JScrollPane stuScroll = new JScrollPane(studentsTable);
        stuScroll.setBorder(null);
        stuCard.add(stuScroll, BorderLayout.CENTER);

        JPanel stuBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stuBtns.setOpaque(false);
        JButton saveScores = new JButton("Save Scores");
        styleButton(saveScores);
        JButton export = new JButton("Export Scores (CSV)");
        styleButton(export);
        JButton imp = new JButton("Import Scores (CSV)");
        styleButton(imp);
        JButton refresh = new JButton("Refresh Students");
        styleButton(refresh);
        JButton editScoreBtn = new JButton("Edit Score");
        styleButton(editScoreBtn);

        saveScores.addActionListener(e -> persistScores());
        export.addActionListener(e -> exportScoresCSV());
        imp.addActionListener(e -> importScoresCSV());
        refresh.addActionListener(e -> {
            try {
                // reload students and keep current in-memory components (if any unsaved changes exist they won't persist)
                // Prefer a full reload from DB to get canonical state
                loadStudentsAndScores(currentSectionId, currentCourseId);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error refreshing students: " + ex.getMessage());
            }
        });

        // Edit Score button: pick selected student (row) and then choose component to edit via dialog
        editScoreBtn.addActionListener(e -> {
            int viewRow = studentsTable.getSelectedRow();
            if (viewRow < 0) {
                JOptionPane.showMessageDialog(this, "Select a student row first to edit a score.");
                return;
            }
            int modelRow = studentsTable.convertRowIndexToModel(viewRow);
            // open dialog with component selector and spinner
            showEditScoreDialogViaSelector(modelRow);
        });

        stuBtns.add(imp);
        stuBtns.add(export);
        stuBtns.add(saveScores);
        stuBtns.add(editScoreBtn);
        stuBtns.add(refresh);

        stuCard.add(stuBtns, BorderLayout.SOUTH);

        split.setBottomComponent(createCard("Students & Scores", stuCard));

        return split;
    }

    private JComponent buildBottom() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.setOpaque(false);

        JButton compute = new JButton("Compute Final Grades");
        styleButton(compute);
        compute.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(this,
                    "Compute final grades for selected section and persist to database?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (res == JOptionPane.YES_OPTION) {
                new SwingWorker<Integer, Void>() {
                    @Override
                    protected Integer doInBackground() throws Exception {
                        return computeAndSaveFinalGrades();
                    }

                    @Override
                    protected void done() {
                        try {
                            int updated = get();
                            JOptionPane.showMessageDialog(InstructorGradesPanel.this,
                                    "Computed and saved final grades for " + updated + " student(s).");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(InstructorGradesPanel.this, "Error: " + ex.getMessage());
                        }
                    }
                }.execute();
            }
        });

        p.add(compute);
        return p;
    }

    // helper: consistent card style same as InstructorDashboard.createCard
    private JPanel createCard(String title, JComponent content) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);

        InstructorDashboard.RoundedPanel tealBg = new InstructorDashboard.RoundedPanel(24, InstructorDashboard.TEAL);
        tealBg.setLayout(new BorderLayout());
        tealBg.setBorder(new EmptyBorder(8, 12, 12, 12));

        JLabel lbl = new JLabel(title);
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setBorder(new EmptyBorder(0, 2, 6, 2));
        tealBg.add(lbl, BorderLayout.NORTH);

        JPanel innerWhite = new JPanel(new BorderLayout());
        innerWhite.setBackground(Color.WHITE);
        innerWhite.setBorder(new EmptyBorder(8, 8, 8, 8));
        innerWhite.add(content, BorderLayout.CENTER);

        tealBg.add(innerWhite, BorderLayout.CENTER);
        outer.add(tealBg, BorderLayout.CENTER);

        return outer;
    }

    private void styleButton(JButton b) {
        b.setBackground(InstructorDashboard.TEAL);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBorder(new EmptyBorder(6, 10, 6, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ---------------- loading / DB logic ----------------

    private void loadSectionsForInstructor() {
        sectionCombo.removeAllItems();
        String sql = "SELECT s.section_id, s.course_id, c.code, c.title, s.semester, s.year " +
                "FROM sections s JOIN courses c ON c.course_id=s.course_id WHERE s.instructor_id = ? ORDER BY c.code";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, instructorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int sid = rs.getInt("section_id");
                    int cid = rs.getInt("course_id");
                    String code = rs.getString("code");
                    String title = rs.getString("title");
                    String sem = rs.getString("semester");
                    String label = code + " - " + title + " (" + sem + ")";
                    sectionCombo.addItem(new SectionItem(sid, cid, sem, label));
                }
            }
            if (sectionCombo.getItemCount() > 0) {
                sectionCombo.setSelectedIndex(0);
            } else {
                status.setText("No sections found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            status.setText("Error loading sections");
        }
    }

    private void loadForSection(int sectionId, int courseId, String semester) {
        currentSectionId = sectionId;
        currentCourseId = courseId;
        currentSemester = semester;
        status.setText("Loading components & students...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // ensure registrations exist (auto-fix)
                ensureRegistrationsForSection(currentSectionId);

                // load components (from grading_components). If none, create defaults from course_grading_scheme.
                currentComponents = loadOrCreateComponents(currentCourseId, currentSectionId);

                // load students and their registration ids and existing grades
                loadStudentsAndScores(currentSectionId, currentCourseId);

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    status.setText("Loaded components and students.");
                } catch (Exception e) {
                    e.printStackTrace();
                    status.setText("Error loading section data.");
                }
            }
        }.execute();
    }

    // Load or create components list
    private List<ComponentDef> loadOrCreateComponents(int courseId, int sectionId) throws Exception {
        List<ComponentDef> comps = new ArrayList<>();
        // check grading_components
        String q = "SELECT component_id, name, max_score, weight_percent, section_id FROM grading_components WHERE course_id = ? AND (section_id = ? OR section_id IS NULL) ORDER BY component_id";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, courseId);
            ps.setInt(2, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ComponentDef cd = new ComponentDef(
                            rs.getInt("component_id"),
                            rs.getString("name"),
                            rs.getBigDecimal("max_score"),
                            rs.getBigDecimal("weight_percent"),
                            rs.getObject("section_id") == null ? null : rs.getInt("section_id")
                    );
                    comps.add(cd);
                }
            }
        }

        if (comps.isEmpty()) {
            // fallback to course_grading_scheme defaults (Quiz/Midsem/Endsem)
            String q2 = "SELECT endsem_weight, midsem_weight, quiz_weight FROM course_grading_scheme WHERE course_id = ?";
            try (Connection conn = DBConnection.getERPConnection();
                 PreparedStatement ps = conn.prepareStatement(q2)) {
                ps.setInt(1, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal endW = rs.getBigDecimal("endsem_weight");
                        BigDecimal midW = rs.getBigDecimal("midsem_weight");
                        BigDecimal quizW = rs.getBigDecimal("quiz_weight");
                        // sensible defaults for max scores (can be edited by instructor)
                        comps.add(new ComponentDef(0, "Quiz", new BigDecimal("10.00"), quizW, sectionId));
                        comps.add(new ComponentDef(0, "Midsem", new BigDecimal("40.00"), midW, sectionId));
                        comps.add(new ComponentDef(0, "Endsem", new BigDecimal("60.00"), endW, sectionId));
                        // persist defaults into grading_components for this course+section
                        insertDefaultComponents(courseId, sectionId, comps);
                    } else {
                        // if no scheme, add empty placeholder
                        comps.add(new ComponentDef(0, "Quiz", new BigDecimal("10.00"), new BigDecimal("20.00"), sectionId));
                        comps.add(new ComponentDef(0, "Midsem", new BigDecimal("40.00"), new BigDecimal("35.00"), sectionId));
                        comps.add(new ComponentDef(0, "Endsem", new BigDecimal("60.00"), new BigDecimal("45.00"), sectionId));
                        insertDefaultComponents(courseId, sectionId, comps);
                    }
                }
            }
        }

        // update componentsModel in EDT
        SwingUtilities.invokeLater(() -> {
            componentsModel.setRowCount(0);
            for (ComponentDef c : comps) {
                componentsModel.addRow(new Object[]{c.componentId, c.name, c.maxScore, c.weightPercent});
            }
        });

        return comps;
    }

    private void insertDefaultComponents(int courseId, int sectionId, List<ComponentDef> comps) throws Exception {
        String ins = "INSERT INTO grading_components (course_id, section_id, name, max_score, weight_percent) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(ins)) {
            for (ComponentDef c : comps) {
                ps.setInt(1, courseId);
                ps.setInt(2, sectionId);
                ps.setString(3, c.name);
                ps.setBigDecimal(4, c.maxScore);
                ps.setBigDecimal(5, c.weightPercent);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // load students and their scores; builds studentsModel dynamically from currentComponents
    private void loadStudentsAndScores(int sectionId, int courseId) throws Exception {
        // build columns: Enrollment(reg_id), Student ID, Roll, Name, then one col per component, then Final, Letter
        List<RowStudent> rows = new ArrayList<>();

        String q = "SELECT e.enrollment_id, e.student_id, s.roll_no, s.name " +
                "FROM enrollments e JOIN students s ON s.user_id = e.student_id WHERE e.section_id = ? ORDER BY s.roll_no";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RowStudent r = new RowStudent();
                    r.enrollmentId = rs.getInt("enrollment_id");
                    r.studentId = rs.getInt("student_id");
                    r.rollNo = rs.getString("roll_no");
                    r.name = rs.getString("name");
                    rows.add(r);
                }
            }
        }

        // map each student -> reg_id for course (registrations)
        String regQ = "SELECT reg_id FROM registrations WHERE user_id = ? AND course_id = ?";
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(regQ)) {
            for (RowStudent r : rows) {
                ps.setInt(1, r.studentId);
                ps.setInt(2, courseId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) r.regId = rs.getInt("reg_id");
                    else r.regId = -1; // shouldn't happen because ensureRegistrationsForSection ran
                }
            }
        }

        // load existing grades for these regIds (grades.enrollment_id == registrations.reg_id)
        Map<Integer, Map<String, BigDecimal>> gradeMap = new HashMap<>();
        if (!rows.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT g.enrollment_id AS reg_id, g.component, g.score, g.max_score FROM grades g WHERE g.enrollment_id IN (");
            for (int i = 0; i < rows.size(); i++) {
                sb.append("?");
                if (i < rows.size() - 1) sb.append(",");
            }
            sb.append(")");
            try (Connection conn = DBConnection.getERPConnection();
                 PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                for (int i = 0; i < rows.size(); i++) ps.setInt(i + 1, rows.get(i).regId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int rid = rs.getInt("reg_id");
                        String comp = rs.getString("component");
                        BigDecimal sc = rs.getBigDecimal("score");
                        gradeMap.computeIfAbsent(rid, k -> new HashMap<>()).put(comp, sc);
                    }
                }
            }
        }

        // build studentsModel columns
        SwingUtilities.invokeLater(() -> {
            // build column names
            Vector<String> cols = new Vector<>();
            cols.add("Reg ID"); // hidden but needed
            cols.add("Student ID");
            cols.add("Roll No");
            cols.add("Name");
            for (ComponentDef c : currentComponents) cols.add(c.name);
            cols.add("Final (%)");
            cols.add("Letter");
            Vector<Vector<Object>> data = new Vector<>();

            // populate rows
            for (RowStudent r : rows) {
                Vector<Object> rowv = new Vector<>();
                rowv.add(r.regId);
                rowv.add(r.studentId);
                rowv.add(r.rollNo);
                rowv.add(r.name);
                Map<String, BigDecimal> gm = gradeMap.getOrDefault(r.regId, Collections.emptyMap());
                for (ComponentDef c : currentComponents) {
                    BigDecimal sc = gm.get(c.name);
                    rowv.add(sc == null ? null : sc);
                }
                rowv.add(null); // Final (%)
                rowv.add(null); // Letter
                data.add(rowv);
            }

            studentsModel.setDataVector(data, cols);

            // hide Reg ID column from view (but keep in model)
            if (studentsTable.getColumnModel().getColumnCount() > 0) {
                try {
                    studentsTable.removeColumn(studentsTable.getColumnModel().getColumn(0));
                } catch (Exception ignore) {}
            }
        });
    }

    // ---------------- persist components ----------------

    private void saveComponents() {
        // read componentsModel into list and validate weights sum to 100
        List<ComponentDef> list = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < componentsModel.getRowCount(); i++) {
            Object idObj = componentsModel.getValueAt(i, 0);
            int compId = (idObj == null) ? 0 : ((Number) idObj).intValue();
            String name = String.valueOf(componentsModel.getValueAt(i, 1));
            BigDecimal max = new BigDecimal(String.valueOf(componentsModel.getValueAt(i, 2)));
            BigDecimal weight = new BigDecimal(String.valueOf(componentsModel.getValueAt(i, 3)));
            sum = sum.add(weight);
            list.add(new ComponentDef(compId, name, max, weight, currentSectionId));
        }
        if (sum.compareTo(new BigDecimal("100")) != 0) {
            JOptionPane.showMessageDialog(this, "Weights must sum to 100. Current sum: " + sum);
            return;
        }

        // persist: insert new, update existing, delete missing (transaction)
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBConnection.getERPConnection()) {
                    conn.setAutoCommit(false);
                    // collect existing ids for this course+section
                    Set<Integer> existing = new HashSet<>();
                    String sel = "SELECT component_id FROM grading_components WHERE course_id = ? AND (section_id = ? OR section_id IS NULL)";
                    try (PreparedStatement ps = conn.prepareStatement(sel)) {
                        ps.setInt(1, currentCourseId);
                        ps.setInt(2, currentSectionId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) existing.add(rs.getInt("component_id"));
                        }
                    }

                    // upsert: update or insert
                    String upd = "UPDATE grading_components SET name=?, max_score=?, weight_percent=?, section_id=? WHERE component_id = ?";
                    String ins = "INSERT INTO grading_components (course_id, section_id, name, max_score, weight_percent) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement psUpd = conn.prepareStatement(upd);
                         PreparedStatement psIns = conn.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                        Set<Integer> seen = new HashSet<>();
                        for (ComponentDef c : list) {
                            if (c.componentId > 0 && existing.contains(c.componentId)) {
                                psUpd.setString(1, c.name);
                                psUpd.setBigDecimal(2, c.maxScore);
                                psUpd.setBigDecimal(3, c.weightPercent);
                                if (c.sectionId == null) psUpd.setNull(4, Types.INTEGER); else psUpd.setInt(4, c.sectionId);
                                psUpd.setInt(5, c.componentId);
                                psUpd.executeUpdate();
                                seen.add(c.componentId);
                            } else {
                                psIns.setInt(1, currentCourseId);
                                if (c.sectionId == null) psIns.setNull(2, Types.INTEGER); else psIns.setInt(2, c.sectionId);
                                psIns.setString(3, c.name);
                                psIns.setBigDecimal(4, c.maxScore);
                                psIns.setBigDecimal(5, c.weightPercent);
                                psIns.executeUpdate();
                                try (ResultSet gk = psIns.getGeneratedKeys()) {
                                    if (gk.next()) {
                                        int gen = gk.getInt(1);
                                        c.componentId = gen;
                                    }
                                }
                            }
                        }
                        // delete any existing components not in seen (for this course+section)
                        for (Integer ex : existing) {
                            if (!seen.contains(ex)) {
                                try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM grading_components WHERE component_id = ?")) {
                                    psDel.setInt(1, ex);
                                    psDel.executeUpdate();
                                }
                            }
                        }
                    }

                    conn.commit();
                    conn.setAutoCommit(true);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    // reload components and students to reflect new component set (from DB)
                    currentComponents = loadOrCreateComponents(currentCourseId, currentSectionId);
                    loadStudentsAndScores(currentSectionId, currentCourseId);
                    JOptionPane.showMessageDialog(InstructorGradesPanel.this, "Saved components.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(InstructorGradesPanel.this, "Error saving components: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ---------------- persist scores ----------------

    private void persistScores() {
        // collects values from studentsModel and upserts into grades table using registrations.reg_id as enrollment_id
        if (currentComponents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No components defined.");
            return;
        }

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                int updated = 0;
                String upsert = "INSERT INTO grades (enrollment_id, component, score, max_score) VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE score=VALUES(score), max_score=VALUES(max_score)";
                try (Connection conn = DBConnection.getERPConnection();
                     PreparedStatement ps = conn.prepareStatement(upsert)) {
                    for (int r = 0; r < studentsModel.getRowCount(); r++) {
                        int modelRegId = (int) studentsModel.getValueAt(r, 0); // reg id stored in model
                        for (int c = 0; c < currentComponents.size(); c++) {
                            Object val = studentsModel.getValueAt(r, 4 + c); // columns: 0 regId,1 studentId,2roll,3name, 4.. = comps
                            BigDecimal score = null;
                            if (val instanceof BigDecimal) score = (BigDecimal) val;
                            else if (val instanceof Number) score = new BigDecimal(((Number) val).toString());
                            else if (val instanceof String && !((String) val).isBlank()) score = new BigDecimal((String) val);
                            ComponentDef cd = currentComponents.get(c);

                            // Edge-case validation prior to persisting: non-negative and <= max
                            if (score != null) {
                                if (score.compareTo(BigDecimal.ZERO) < 0) {
                                    throw new IllegalArgumentException("Negative score for reg_id " + modelRegId + " component " + cd.name);
                                }
                                if (cd.maxScore != null && score.compareTo(cd.maxScore) > 0) {
                                    throw new IllegalArgumentException("Score exceeds max for reg_id " + modelRegId + " component " + cd.name);
                                }
                            }

                            ps.setInt(1, modelRegId);
                            ps.setString(2, cd.name);
                            if (score == null) ps.setNull(3, Types.DECIMAL); else ps.setBigDecimal(3, score);
                            ps.setBigDecimal(4, cd.maxScore);
                            ps.addBatch();
                        }
                    }
                    int[] res = ps.executeBatch();
                    for (int v : res) if (v >= 0) updated++;
                }
                return updated;
            }

            @Override
            protected void done() {
                try {
                    int updated = get();
                    JOptionPane.showMessageDialog(InstructorGradesPanel.this, "Saved scores (batch updates: " + updated + ").");
                    // reload grades to refresh
                    loadStudentsAndScores(currentSectionId, currentCourseId);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(InstructorGradesPanel.this, "Error saving scores: " + e.getMessage());
                }
            }
        }.execute();
    }

    // ---------------- compute final grades ----------------

    private int computeAndSaveFinalGrades() throws Exception {
        // load components and weights
        List<ComponentDef> comps = currentComponents;
        if (comps.isEmpty()) throw new IllegalStateException("No components defined.");

        // fetch all students' reg ids and scores from DB for this course+section
        List<Integer> regIds = new ArrayList<>();
        for (int r = 0; r < studentsModel.getRowCount(); r++) regIds.add((Integer) studentsModel.getValueAt(r, 0));
        if (regIds.isEmpty()) return 0;

        // prepare query to fetch all grades for these regIds in one shot
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT enrollment_id, component, score, max_score FROM grades WHERE enrollment_id IN (");
        for (int i = 0; i < regIds.size(); i++) {
            sb.append("?");
            if (i < regIds.size() - 1) sb.append(",");
        }
        sb.append(")");
        Map<Integer, Map<String, BigDecimal>> gradeMap = new HashMap<>();
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < regIds.size(); i++) ps.setInt(i + 1, regIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rid = rs.getInt("enrollment_id");
                    String comp = rs.getString("component");
                    BigDecimal score = rs.getBigDecimal("score");
                    gradeMap.computeIfAbsent(rid, k -> new HashMap<>()).put(comp, score);
                }
            }
        }

        // compute for each regId
        int saved = 0;
        try (Connection conn = DBConnection.getERPConnection()) {
            conn.setAutoCommit(false);
            for (int r = 0; r < studentsModel.getRowCount(); r++) {
                int regId = (Integer) studentsModel.getValueAt(r, 0);
                Map<String, BigDecimal> gm = gradeMap.getOrDefault(regId, Collections.emptyMap());

                // compute weighted sum: sum (score / max) * weight
                BigDecimal total = BigDecimal.ZERO;
                for (ComponentDef c : comps) {
                    BigDecimal sc = gm.get(c.name);
                    BigDecimal s = (sc == null) ? BigDecimal.ZERO : sc;
                    BigDecimal contrib = BigDecimal.ZERO;
                    if (c.maxScore != null && c.maxScore.compareTo(BigDecimal.ZERO) > 0) {
                        contrib = s.divide(c.maxScore, 6, BigDecimal.ROUND_HALF_UP).multiply(c.weightPercent);
                    }
                    total = total.add(contrib);
                }
                BigDecimal finalNumeric = total.setScale(2, BigDecimal.ROUND_HALF_UP);
                String letter = numericToLetter(finalNumeric.doubleValue());

                // get user_id from registrations
                int userId;
                try (PreparedStatement psReg = conn.prepareStatement("SELECT user_id FROM registrations WHERE reg_id = ?")) {
                    psReg.setInt(1, regId);
                    try (ResultSet rs = psReg.executeQuery()) {
                        if (rs.next()) userId = rs.getInt("user_id");
                        else continue;
                    }
                }

                Integer fgId = null;
                try (PreparedStatement psCheck = conn.prepareStatement("SELECT fg_id FROM final_grades WHERE user_id = ? AND course_id = ?")) {
                    psCheck.setInt(1, userId);
                    psCheck.setInt(2, currentCourseId);
                    try (ResultSet rs = psCheck.executeQuery()) {
                        if (rs.next()) fgId = rs.getInt("fg_id");
                    }
                }

                if (fgId == null) {
                    try (PreparedStatement psIns = conn.prepareStatement("INSERT INTO final_grades (user_id, course_id, semester, numeric_grade, letter_grade) VALUES (?, ?, ?, ?, ?)")) {
                        psIns.setInt(1, userId);
                        psIns.setInt(2, currentCourseId);
                        psIns.setString(3, currentSemester == null ? "" : currentSemester);
                        psIns.setBigDecimal(4, finalNumeric);
                        psIns.setString(5, letter);
                        psIns.executeUpdate();
                        saved++;
                    }
                } else {
                    try (PreparedStatement psUpd = conn.prepareStatement("UPDATE final_grades SET numeric_grade = ?, letter_grade = ? WHERE fg_id = ?")) {
                        psUpd.setBigDecimal(1, finalNumeric);
                        psUpd.setString(2, letter);
                        psUpd.setInt(3, fgId);
                        psUpd.executeUpdate();
                        saved++;
                    }
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        }

        // reload students table to show final% and letter
        loadStudentsAndScores(currentSectionId, currentCourseId);

        // update UI model final columns from final_grades so instructor sees results:
        SwingUtilities.invokeLater(() -> {
            for (int r = 0; r < studentsModel.getRowCount(); r++) {
                int regId = (Integer) studentsModel.getValueAt(r, 0);
                int userId;
                try (Connection conn = DBConnection.getERPConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM registrations WHERE reg_id = ?")) {
                    ps.setInt(1, regId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) userId = rs.getInt("user_id");
                        else continue;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }

                try (Connection conn = DBConnection.getERPConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT numeric_grade, letter_grade FROM final_grades WHERE user_id = ? AND course_id = ?")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, currentCourseId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            BigDecimal ng = rs.getBigDecimal("numeric_grade");
                            String lg = rs.getString("letter_grade");
                            studentsModel.setValueAt(ng, r, 4 + currentComponents.size()); // Final (%) column index
                            studentsModel.setValueAt(lg, r, 5 + currentComponents.size()); // Letter
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        return saved;
    }

    // numeric to letter mapping
    private String numericToLetter(double x) {
        if (x >= 90) return "A+";
        if (x >= 80) return "A";
        if (x >= 75) return "A-";
        if (x >= 70) return "B+";
        if (x >= 60) return "B";
        if (x >= 50) return "C";
        if (x >= 40) return "D";
        return "F";
    }

    // ---------------- CSV import/export simple implementations ----------------

    private void exportScoresCSV() {
        if (studentsModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No students to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("scores_section_" + currentSectionId + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (FileWriter w = new FileWriter(chooser.getSelectedFile())) {
            // header
            StringBuilder hdr = new StringBuilder();
            hdr.append("RegID,StudentID,Roll,Name");
            for (ComponentDef c : currentComponents) hdr.append(",").append(c.name);
            hdr.append(",Final,Letter\n");
            w.write(hdr.toString());
            for (int r = 0; r < studentsModel.getRowCount(); r++) {
                StringBuilder row = new StringBuilder();
                row.append(studentsModel.getValueAt(r, 0)).append(","); // reg id
                row.append(studentsModel.getValueAt(r, 1)).append(",");
                row.append("\"").append(studentsModel.getValueAt(r, 2)).append("\",");
                row.append("\"").append(studentsModel.getValueAt(r, 3)).append("\"");
                for (int c = 0; c < currentComponents.size(); c++) {
                    Object v = studentsModel.getValueAt(r, 4 + c);
                    row.append(",");
                    row.append(v == null ? "" : v.toString());
                }
                row.append(",");
                row.append(studentsModel.getValueAt(r, 4 + currentComponents.size()) == null ? "" : studentsModel.getValueAt(r, 4 + currentComponents.size()).toString());
                row.append(",");
                row.append(studentsModel.getValueAt(r, 5 + currentComponents.size()) == null ? "" : studentsModel.getValueAt(r, 5 + currentComponents.size()).toString());
                row.append("\n");
                w.write(row.toString());
            }
            JOptionPane.showMessageDialog(this, "Exported CSV: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting CSV: " + e.getMessage());
        }
    }

    private void importScoresCSV() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (BufferedReader br = new BufferedReader(new FileReader(chooser.getSelectedFile()))) {
            String header = br.readLine();
            if (header == null) return;
            // naive CSV parse: split by comma (ok for simple templates)
            String[] cols = header.split(",");
            Map<String, Integer> compIndex = new HashMap<>();
            for (int i = 0; i < cols.length; i++) compIndex.put(cols[i].trim(), i);
            String line;
            int updated = 0;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",");
                // require RegID
                int regId = Integer.parseInt(vals[0].trim());
                // find row in studentsModel by regId
                int rowIndex = -1;
                for (int r = 0; r < studentsModel.getRowCount(); r++) {
                    if (((Integer) studentsModel.getValueAt(r, 0)) == regId) {
                        rowIndex = r;
                        break;
                    }
                }
                if (rowIndex < 0) continue;
                // set component scores
                for (int c = 0; c < currentComponents.size(); c++) {
                    String cname = currentComponents.get(c).name;
                    Integer csvIdx = compIndex.get(cname);
                    if (csvIdx == null) continue;
                    String raw = csvIdx < vals.length ? vals[csvIdx].trim() : "";
                    if (!raw.isBlank()) {
                        BigDecimal parsed = new BigDecimal(raw);
                        // validate against max
                        BigDecimal max = currentComponents.get(c).maxScore;
                        if (parsed.compareTo(BigDecimal.ZERO) < 0) continue; // skip negative
                        if (max != null && parsed.compareTo(max) > 0) continue; // skip > max
                        studentsModel.setValueAt(parsed, rowIndex, 4 + c);
                        updated++;
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "Imported scores (local model updated): " + updated);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error importing CSV: " + e.getMessage());
        }
    }

    // ---------------- helper utilities ----------------

    // create grading_components table if missing
    private void ensureGradingComponentsTable() {
        String ddl = """
                CREATE TABLE IF NOT EXISTS grading_components (
                  component_id INT NOT NULL AUTO_INCREMENT,
                  course_id INT NOT NULL,
                  section_id INT DEFAULT NULL,
                  name VARCHAR(100) NOT NULL,
                  max_score DECIMAL(8,2) NOT NULL,
                  weight_percent DECIMAL(5,2) NOT NULL,
                  PRIMARY KEY (component_id),
                  UNIQUE KEY uq_comp_course_section_name (course_id, section_id, name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;
        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(ddl)) {
            ps.execute();
        } catch (Exception e) {
            // ignore if permission denied or other; panel will still work if DB schema created manually
            e.printStackTrace();
        }
    }

    // Ensure registrations exist for all students in section (same helper we gave earlier)
    public static int ensureRegistrationsForSection(int sectionId) throws Exception {
        String findEnrollmentsSql = """
                SELECT e.enrollment_id, e.student_id, sec.course_id
                FROM enrollments e
                JOIN sections sec ON sec.section_id = e.section_id
                WHERE e.section_id = ?
                """;

        String checkRegSql = "SELECT reg_id FROM registrations WHERE user_id = ? AND course_id = ?";
        String insertRegSql = "INSERT INTO registrations(course_id, user_id) VALUES (?, ?)";

        int created = 0;

        try (Connection conn = DBConnection.getERPConnection();
             PreparedStatement psEnroll = conn.prepareStatement(findEnrollmentsSql)) {

            conn.setAutoCommit(false);

            psEnroll.setInt(1, sectionId);
            try (ResultSet rs = psEnroll.executeQuery();
                 PreparedStatement psCheck = conn.prepareStatement(checkRegSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertRegSql)) {

                while (rs.next()) {
                    int studentId = rs.getInt("student_id");
                    int courseId = rs.getInt("course_id");

                    psCheck.setInt(1, studentId);
                    psCheck.setInt(2, courseId);
                    try (ResultSet rs2 = psCheck.executeQuery()) {
                        if (!rs2.next()) {
                            psInsert.setInt(1, courseId);
                            psInsert.setInt(2, studentId);
                            psInsert.executeUpdate();
                            created++;
                        }
                    }
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception ex) {
            throw ex;
        }
        return created;
    }

    // ---------------- Edit Component dialog (reuse) ----------------

    private void showEditComponentDialog(int modelRow) {
        if (modelRow < 0 || modelRow >= componentsModel.getRowCount()) return;

        Object idObj = componentsModel.getValueAt(modelRow, 0);
        int compId = (idObj == null) ? 0 : ((Number) idObj).intValue();
        String currentName = String.valueOf(componentsModel.getValueAt(modelRow, 1));
        BigDecimal currentMax = new BigDecimal(String.valueOf(componentsModel.getValueAt(modelRow, 2)));
        BigDecimal currentWeight = new BigDecimal(String.valueOf(componentsModel.getValueAt(modelRow, 3)));

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        panel.add(new JLabel("Component name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(currentName, 18);
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Max score:"), gbc);
        gbc.gridx = 1;
        SpinnerNumberModel maxModel = new SpinnerNumberModel(currentMax.doubleValue(), 0.01, Double.MAX_VALUE, 1.0);
        JSpinner maxSpinner = new JSpinner(maxModel);
        ((JSpinner.NumberEditor) maxSpinner.getEditor()).getTextField().setColumns(10);
        panel.add(maxSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Weight (%):"), gbc);
        gbc.gridx = 1;
        SpinnerNumberModel weightModel = new SpinnerNumberModel(currentWeight.doubleValue(), 0.0, 100.0, 0.5);
        JSpinner weightSpinner = new JSpinner(weightModel);
        ((JSpinner.NumberEditor) weightSpinner.getEditor()).getTextField().setColumns(10);
        panel.add(weightSpinner, gbc);

        nameField.requestFocusInWindow();
        int res = JOptionPane.showConfirmDialog(this, panel, "Edit Component", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        double maxVal = ((Number) maxSpinner.getValue()).doubleValue();
        double weightVal = ((Number) weightSpinner.getValue()).doubleValue();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Component name cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (maxVal <= 0) {
            JOptionPane.showMessageDialog(this, "Max score must be positive.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (weightVal < 0) {
            JOptionPane.showMessageDialog(this, "Weight cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // duplicate name check (case-insensitive) against other rows
        for (int i = 0; i < componentsModel.getRowCount(); i++) {
            if (i == modelRow) continue;
            Object existing = componentsModel.getValueAt(i, 1);
            if (existing != null && name.equalsIgnoreCase(existing.toString().trim())) {
                JOptionPane.showMessageDialog(this, "A component with this name already exists.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        // weight sum check: ensure sum of other weights + new weight <= 100
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < componentsModel.getRowCount(); i++) {
            if (i == modelRow) continue;
            Object v = componentsModel.getValueAt(i, 3);
            if (v != null) {
                try {
                    sum = sum.add(new BigDecimal(String.valueOf(v)));
                } catch (Exception ex) {
                    /* ignore */
                }
            }
        }
        BigDecimal newSum = sum.add(BigDecimal.valueOf(weightVal));
        if (newSum.compareTo(new BigDecimal("100")) > 0) {
            JOptionPane.showMessageDialog(this,
                    "Total weight would exceed 100%. Current sum of other components: " + sum + "%. After change: " + newSum + "%",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        BigDecimal maxBd = BigDecimal.valueOf(maxVal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal weightBd = BigDecimal.valueOf(weightVal).setScale(2, RoundingMode.HALF_UP);

        componentsModel.setValueAt(name, modelRow, 1);
        componentsModel.setValueAt(maxBd, modelRow, 2);
        componentsModel.setValueAt(weightBd, modelRow, 3);

        // update currentComponents entry at same index if present
        if (modelRow >= 0 && modelRow < currentComponents.size()) {
            ComponentDef cd = currentComponents.get(modelRow);
            cd.name = name;
            cd.maxScore = maxBd;
            cd.weightPercent = weightBd;
        } else {
            boolean found = false;
            for (ComponentDef cd : currentComponents) {
                if (cd.componentId == compId) {
                    cd.name = name;
                    cd.maxScore = maxBd;
                    cd.weightPercent = weightBd;
                    found = true;
                    break;
                }
            }
            if (!found) currentComponents.add(new ComponentDef(compId, name, maxBd, weightBd, currentSectionId));
        }
    }

    // rebuild studentsModel using currentComponents list (preserves existing student rows and their existing comp values)
    // oldComponentCount must be provided so we can map old values correctly (pass previous count before modification)
    @SuppressWarnings("unchecked")
    private void rebuildStudentsModelWithCurrentComponents(int oldComponentCount) {
        SwingUtilities.invokeLater(() -> {
            // capture existing row and final/letter values
            int rows = studentsModel.getRowCount();
            Vector<Vector<Object>> newData = new Vector<>();
            for (int r = 0; r < rows; r++) {
                Vector<Object> newRow = new Vector<>();
                // regId, studentId, roll, name
                newRow.add(studentsModel.getValueAt(r, 0));
                newRow.add(studentsModel.getValueAt(r, 1));
                newRow.add(studentsModel.getValueAt(r, 2));
                newRow.add(studentsModel.getValueAt(r, 3));
                // existing component values up to oldComponentCount
                for (int c = 0; c < currentComponents.size(); c++) {
                    if (c < oldComponentCount) {
                        // old value was at index 4 + c
                        Object v = studentsModel.getValueAt(r, 4 + c);
                        newRow.add(v);
                    } else {
                        // new component (unsaved) -> null
                        newRow.add(null);
                    }
                }
                // Final (%) and Letter - old indices were 4+oldCount and 5+oldCount
                Object finalVal = null;
                Object letterVal = null;
                try {
                    finalVal = (oldComponentCount >= 0 && (4 + oldComponentCount) < studentsModel.getColumnCount()) ? studentsModel.getValueAt(r, 4 + oldComponentCount) : null;
                } catch (Exception ignored) {}
                try {
                    letterVal = (oldComponentCount >= 0 && (5 + oldComponentCount) < studentsModel.getColumnCount()) ? studentsModel.getValueAt(r, 5 + oldComponentCount) : null;
                } catch (Exception ignored) {}

                newRow.add(finalVal);
                newRow.add(letterVal);

                newData.add(newRow);
            }

            // new column headers
            Vector<String> cols = new Vector<>();
            cols.add("Reg ID");
            cols.add("Student ID");
            cols.add("Roll No");
            cols.add("Name");
            for (ComponentDef c : currentComponents) cols.add(c.name);
            cols.add("Final (%)");
            cols.add("Letter");

            studentsModel.setDataVector(newData, cols);

            // hide Reg ID column from view (but keep in model)
            if (studentsTable.getColumnModel().getColumnCount() > 0) {
                try {
                    studentsTable.removeColumn(studentsTable.getColumnModel().getColumn(0));
                } catch (Exception ignore) {}
            }
        });
    }

    // ---------------- Edit single score via selector dialog ----------------
    // modelRow - index in model (not view)
    private void showEditScoreDialogViaSelector(int modelRow) {
        if (modelRow < 0 || modelRow >= studentsModel.getRowCount()) return;
        if (currentComponents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No components available to edit.");
            return;
        }

        Object studentName = studentsModel.getValueAt(modelRow, 3);
        Object regIdObj = studentsModel.getValueAt(modelRow, 0);
        int regId = (regIdObj instanceof Integer) ? (Integer) regIdObj : -1;

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        panel.add(new JLabel("Student:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(String.valueOf(studentName)), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Component:"), gbc);
        gbc.gridx = 1;
        String[] compNames = currentComponents.stream().map(c -> c.name).toArray(String[]::new);
        JComboBox<String> compBox = new JComboBox<>(compNames);
        panel.add(compBox, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Max score:"), gbc);
        gbc.gridx = 1;
        JLabel maxLabel = new JLabel();
        panel.add(maxLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Enter score:"), gbc);
        gbc.gridx = 1;
        SpinnerNumberModel scoreModel = new SpinnerNumberModel(0.0, -99999.0, 999999.0, 0.5);
        JSpinner scoreSpinner = new JSpinner(scoreModel);
        ((JSpinner.NumberEditor) scoreSpinner.getEditor()).getTextField().setColumns(10);
        panel.add(scoreSpinner, gbc);

        // update maxLabel and spinner initial value when component selection changes
        compBox.addActionListener(e -> {
            int idx = compBox.getSelectedIndex();
            if (idx >= 0 && idx < currentComponents.size()) {
                ComponentDef cd = currentComponents.get(idx);
                maxLabel.setText(cd.maxScore == null ? "-" : cd.maxScore.toPlainString());
                // set spinner initial to existing value if present
                Object existing = null;
                try { existing = studentsModel.getValueAt(modelRow, 4 + idx); } catch (Exception ignored) {}
                double init = 0.0;
                if (existing instanceof BigDecimal) init = ((BigDecimal) existing).doubleValue();
                else if (existing instanceof Number) init = ((Number) existing).doubleValue();
                scoreSpinner.setValue(init);
            }
        });
        // trigger initial update
        compBox.setSelectedIndex(0);

        int res = JOptionPane.showConfirmDialog(this, panel, "Edit Score", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        int selIdx = compBox.getSelectedIndex();
        if (selIdx < 0 || selIdx >= currentComponents.size()) return;
        ComponentDef comp = currentComponents.get(selIdx);
        double entered = ((Number) scoreSpinner.getValue()).doubleValue();
        BigDecimal enteredBd = BigDecimal.valueOf(entered).setScale(2, RoundingMode.HALF_UP);

        // validations
        if (enteredBd.compareTo(BigDecimal.ZERO) < 0) {
            JOptionPane.showMessageDialog(this, "Score cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (comp.maxScore != null && enteredBd.compareTo(comp.maxScore) > 0) {
            JOptionPane.showMessageDialog(this, "Score cannot exceed component max (" + comp.maxScore + ").", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // set into model (will be saved to DB when Save Scores clicked)
        studentsModel.setValueAt(enteredBd, modelRow, 4 + selIdx);
    }

    // ---------------- small data classes ----------------

    private static class SectionItem {
        final int sectionId;
        final int courseId;
        final String semester;
        final String label;

        SectionItem(int s, int c, String sem, String lbl) {
            sectionId = s;
            courseId = c;
            semester = sem;
            label = lbl;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static class ComponentDef {
        int componentId;
        String name;
        BigDecimal maxScore;
        BigDecimal weightPercent;
        Integer sectionId; // nullable

        ComponentDef(int id, String n, BigDecimal max, BigDecimal w, Integer sec) {
            componentId = id;
            name = n;
            maxScore = max;
            weightPercent = w;
            sectionId = sec;
        }
    }

    private static class RowStudent {
        int enrollmentId;
        int studentId;
        int regId;
        String rollNo;
        String name;
    }
}
