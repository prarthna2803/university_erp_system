package erp.ui.instructor;

import erp.service.InstructorService.SectionRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

class InstructorCoursesPanel extends JPanel {

    private final InstructorContext context;
    private final JTable sectionsTable = new JTable();
    private final JButton openGradebookBtn = new JButton("Open Gradebook");

    InstructorCoursesPanel(InstructorContext context) {
        this.context = context;
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(24, 24, 24, 24));
        setBackground(new Color(0xEEF3F9));

        JLabel heading = new JLabel("My Courses");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
        add(heading, BorderLayout.NORTH);

        sectionsTable.setFillsViewportHeight(true);
        sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(sectionsTable), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        openGradebookBtn.addActionListener(e -> openGradebook());
        footer.add(refresh);
        footer.add(openGradebookBtn);

        if (context.maintenanceService.isMaintenanceMode()) {
            JLabel banner = new JLabel("Maintenance mode: grade entry is disabled.");
            banner.setForeground(new Color(0xB54747));
            banner.setBorder(new EmptyBorder(0, 0, 0, 20));
            footer.add(banner, 0);
            openGradebookBtn.setEnabled(false);
        }

        add(footer, BorderLayout.SOUTH);
        refresh();
    }

    private void refresh() {
        List<SectionRow> sections = context.instructorService.listMySections(context.userId);
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Course", "Section ID", "Schedule", "Room"}, 0);

        sections.forEach(row -> {
            String fullCourse = row.courseCode() + " - " + row.courseTitle(); // CS101 - DS
            model.addRow(new Object[]{
                    fullCourse,
                    row.sectionId(),
                    row.dayTime(),
                    row.room()
            });
        });

        sectionsTable.setModel(model);
    }

    private void openGradebook() {
        int row = sectionsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a section first.");
            return;
        }
        int sectionId = (int) sectionsTable.getValueAt(row, 1);
        String course = (String) sectionsTable.getValueAt(row, 0);
        new InstructorGradesPanel(context, sectionId, course).setVisible(true);
    }
}
