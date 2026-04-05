package erp.ui.instructor;

import erp.data.DBConnection;
import erp.ui.instructor.InstructorDashboard.RoundedPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InstructorSectionsPanel extends JPanel {

    private final InstructorDashboard dashboard;
    private final int instructorId;

    private JTable sectionsTable;
    private JTable enrolledTable;

    private DefaultTableModel sectionsModel;
    private DefaultTableModel enrolledModel;

    private JLabel status;

    public InstructorSectionsPanel(InstructorDashboard dashboard, int instructorId) {
        this.dashboard = dashboard;
        this.instructorId = instructorId;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20,20,20,20));

        JPanel wrapper = new RoundedPanel(24, InstructorDashboard.TEAL);
        wrapper.setLayout(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(14,14,14,14));

        wrapper.add(buildTop(), BorderLayout.NORTH);
        wrapper.add(buildSplit(), BorderLayout.CENTER);
        wrapper.add(buildBottom(), BorderLayout.SOUTH);

        add(wrapper, BorderLayout.CENTER);

        loadSections();
    }

    // ============================================================
    private JComponent buildTop() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("My Sections");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        top.add(title);

        top.add(Box.createVerticalStrut(6));

        status = new JLabel(" ");
        status.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        status.setForeground(Color.WHITE);
        status.setAlignmentX(LEFT_ALIGNMENT);
        top.add(status);

        if (dashboard.isMaintenanceOn()) {
            JPanel banner = new JPanel(new BorderLayout());
            banner.setBackground(new Color(255,230,160));
            banner.setBorder(new EmptyBorder(6,10,6,10));
            banner.add(new JLabel("Maintenance Mode: View only."), BorderLayout.WEST);
            banner.setAlignmentX(LEFT_ALIGNMENT);

            top.add(Box.createVerticalStrut(8));
            top.add(banner);
        }

        return top;
    }

    // ============================================================
    private JComponent buildSplit() {
        JPanel inner = new JPanel(new BorderLayout());
        inner.setBackground(Color.WHITE);
        inner.setBorder(new EmptyBorder(10,10,10,10));

        sectionsModel = new DefaultTableModel(new String[]{
                "Section ID","Course","Title","Day","Time","Room","Capacity",
                "Semester","Year","Enrolled"
        },0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };

        sectionsTable = new JTable(sectionsModel);
        sectionsTable.setRowHeight(22);
        sectionsTable.getSelectionModel().addListSelectionListener(
                e -> { if(!e.getValueIsAdjusting()) loadEnrolled(); });

        JScrollPane secScroll = new JScrollPane(sectionsTable);
        secScroll.getViewport().setBackground(Color.WHITE);
        secScroll.setBorder(null);

        // NO manual column width settings as requested
        // sectionsTable uses default auto-resize behavior

        enrolledModel = new DefaultTableModel(new String[]{
                "Student ID","Roll No","Name","Program"
        },0){
            @Override public boolean isCellEditable(int r,int c){return false;}
        };

        enrolledTable = new JTable(enrolledModel);
        enrolledTable.setRowHeight(22);

        JScrollPane stuScroll = new JScrollPane(enrolledTable);
        stuScroll.setBorder(null);
        stuScroll.getViewport().setBackground(Color.WHITE);

        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, secScroll, stuScroll);
        sp.setResizeWeight(0.55);
        sp.setBorder(null);

        inner.add(sp, BorderLayout.CENTER);
        return inner;
    }

    // ============================================================
    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton refresh = styledButton("Refresh");
        refresh.addActionListener(e -> reload());

        JButton csv = styledButton("Download CSV");
        csv.addActionListener(e -> exportCSV());

        bottom.add(refresh);
        bottom.add(csv);

        return bottom;
    }

    private JButton styledButton(String text){
        JButton b = new JButton(text);
        b.setBackground(InstructorDashboard.TEAL);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI",Font.BOLD,14));
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8,20,8,20));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ============================================================
    private void loadSections() {
        sectionsModel.setRowCount(0);

        String sql = """
            SELECT s.section_id, c.code, c.title, s.day, s.time, s.room,
                   s.capacity, s.semester, s.year,
                   (SELECT COUNT(*) FROM enrollments e WHERE e.section_id=s.section_id) AS enrolled
            FROM sections s
            JOIN courses c ON c.course_id=s.course_id
            WHERE s.instructor_id=?
            ORDER BY s.year DESC, s.semester, c.code
        """;

        try(Connection conn = DBConnection.getERPConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, instructorId);
            ResultSet rs = ps.executeQuery();

            int count = 0;
            while(rs.next()){
                sectionsModel.addRow(new Object[]{
                        rs.getInt("section_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("day"),
                        rs.getString("time"),
                        rs.getString("room"),
                        rs.getObject("capacity") == null ? null : rs.getInt("capacity"),
                        rs.getString("semester"),
                        rs.getObject("year") == null ? null : rs.getInt("year"),
                        rs.getInt("enrolled")
                });
                count++;
            }

            status.setText(count + " section(s)");

            if(count > 0){
                sectionsTable.setRowSelectionInterval(0,0);
                loadEnrolled();
            } else {
                enrolledModel.setRowCount(0);
                status.setText("No sections found.");
            }

        } catch(Exception e){
            e.printStackTrace();
            status.setText("Error loading sections");
        }
    }

    // ============================================================
    private void loadEnrolled(){
        enrolledModel.setRowCount(0);

        int row = sectionsTable.getSelectedRow();
        if(row < 0) {
            status.setText(status.getText()); // keep existing status
            return;
        }

        int mid = sectionsTable.convertRowIndexToModel(row);
        Object idObj = sectionsModel.getValueAt(mid, 0);
        if (idObj == null) return;

        final int sectionId;
        if (idObj instanceof Number) {
            sectionId = ((Number) idObj).intValue();
        } else {
            try {
                sectionId = Integer.parseInt(idObj.toString());
            } catch (NumberFormatException ex) {
                ex.printStackTrace();
                return;
            }
        }

        status.setText("Loading students...");
        new SwingWorker<Integer, Object[]>() {
            @Override
            protected Integer doInBackground() throws Exception {
                String sql = """
                    SELECT s.user_id, s.roll_no, s.name, s.program
                    FROM enrollments e
                    JOIN students s ON s.user_id = e.student_id
                    WHERE e.section_id=?
                    ORDER BY s.roll_no
                """;
                int count = 0;
                try (Connection conn = DBConnection.getERPConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, sectionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Object[] rowData = new Object[]{
                                    rs.getInt("user_id"),
                                    rs.getString("roll_no"),
                                    rs.getString("name"),
                                    rs.getString("program")
                            };
                            publish(rowData);
                            count++;
                        }
                    }
                }
                return count;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] r : chunks) enrolledModel.addRow(r);
            }

            @Override
            protected void done() {
                try {
                    int c = get();
                    if (c == 0) {
                        status.setText("0 students enrolled in the selected section.");
                    } else {
                        status.setText(c + " student(s)");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    status.setText("Error loading students");
                }
            }
        }.execute();
    }

    // ============================================================
    private void exportCSV(){
        if(sectionsModel.getRowCount()==0){
            JOptionPane.showMessageDialog(this,"Nothing to export.");
            return;
        }

        String[] options = {"Sections list (all)","Enrolled students (selected)","Both"};
        int choice = JOptionPane.showOptionDialog(this,
                "Choose CSV export:",
                "Export CSV",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (choice == 0) {
            exportSectionsToFile();
        } else if (choice == 1) {
            exportSelectedEnrolledToFile();
        } else if (choice == 2) {
            exportSectionsToFile();
            exportSelectedEnrolledToFile();
        }
    }

    private void exportSectionsToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("sections.csv"));
        if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try(FileWriter w = new FileWriter(chooser.getSelectedFile())){
            w.write("Section ID,Course Code,Title,Day,Time,Room,"
                    +"Capacity,Semester,Year,Enrolled\n");

            for(int i=0;i<sectionsModel.getRowCount();i++){
                w.write(
                        sectionsModel.getValueAt(i,0)+","+
                                sectionsModel.getValueAt(i,1)+","+
                                "\""+sectionsModel.getValueAt(i,2)+"\","+
                                sectionsModel.getValueAt(i,3)+","+
                                sectionsModel.getValueAt(i,4)+","+
                                sectionsModel.getValueAt(i,5)+","+
                                sectionsModel.getValueAt(i,6)+","+
                                sectionsModel.getValueAt(i,7)+","+
                                sectionsModel.getValueAt(i,8)+","+
                                sectionsModel.getValueAt(i,9)+"\n"
                );
            }
            JOptionPane.showMessageDialog(this,"Sections CSV saved: " + chooser.getSelectedFile().getAbsolutePath());

        } catch(Exception e){
            JOptionPane.showMessageDialog(this,"Error saving sections CSV: "+e.getMessage());
        }
    }

    private void exportSelectedEnrolledToFile() {
        int row = sectionsTable.getSelectedRow();
        if(row < 0) {
            JOptionPane.showMessageDialog(this,"Select a section first to export enrolled students.");
            return;
        }
        int mid = sectionsTable.convertRowIndexToModel(row);
        Object idObj = sectionsModel.getValueAt(mid, 0);
        if (idObj == null) {
            JOptionPane.showMessageDialog(this,"Invalid section selected.");
            return;
        }
        final int sectionId = (idObj instanceof Number) ? ((Number)idObj).intValue() : Integer.parseInt(idObj.toString());

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("enrolled_section_" + sectionId + ".csv"));
        if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String sql = """
            SELECT s.user_id, s.roll_no, s.name, s.program, s.email, s.phone
            FROM enrollments e
            JOIN students s ON s.user_id = e.student_id
            WHERE e.section_id=?
            ORDER BY s.roll_no
        """;

        try (FileWriter w = new FileWriter(chooser.getSelectedFile());
             Connection conn = DBConnection.getERPConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sectionId);
            try (ResultSet rs = ps.executeQuery()) {
                w.write("Student ID,Roll No,Name,Program,Email,Phone\n");
                int rows = 0;
                while (rs.next()) {
                    String name = rs.getString("name");
                    String roll = rs.getString("roll_no");
                    String prog = rs.getString("program");
                    String email = rs.getString("email");
                    String phone = rs.getString("phone");
                    w.write(rs.getInt("user_id") + "," +
                            "\"" + roll + "\"," +
                            "\"" + (name == null ? "" : name) + "\"," +
                            "\"" + (prog == null ? "" : prog) + "\"," +
                            "\"" + (email == null ? "" : email) + "\"," +
                            "\"" + (phone == null ? "" : phone) + "\"" +
                            "\n");
                    rows++;
                }
                JOptionPane.showMessageDialog(this, rows + " enrolled student(s) exported to:\n" + chooser.getSelectedFile().getAbsolutePath());
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,"Error exporting enrolled CSV: " + e.getMessage());
        }
    }

    // ============================================================
    public void reload() {
        loadSections();
    }
}
