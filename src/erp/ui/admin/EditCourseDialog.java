package erp.ui.admin;

import erp.data.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class EditCourseDialog extends JDialog {

    private final AdminCoursesPanel parent;
    private final int sectionId;
    private final int courseId;

    private JTextField txtCode, txtName, txtInstructor, txtTitle, txtCredits,
            txtRegD, txtDropD, txtDay, txtStart, txtEnd, txtRoom, txtCapacity, txtSem, txtYear;

    public EditCourseDialog(AdminCoursesPanel parent,
                            int sectionId, int courseId,
                            String code, String title, int credits,
                            String instructor, String dayTime, String room,
                            int capacity, String semester, int year,
                            String reg, String drop) {

        this.parent = parent;
        this.sectionId = sectionId;
        this.courseId = courseId;

        setTitle("Edit Course");
        setModal(true);
        setSize(450, 650);
        setLocationRelativeTo(parent);

        String[] dt = dayTime.split(" ");
        String day = dt[0];
        String[] times = dt[1].split("–");

        String start = times[0];
        String end = times[1];

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20,20,20,20));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8,8,8,8);
        c.fill = GridBagConstraints.HORIZONTAL;

        txtCode = new JTextField(code);
        txtName = new JTextField(title);
        txtInstructor = new JTextField(instructor);
        txtTitle = new JTextField(title);
        txtCredits = new JTextField(String.valueOf(credits));
        txtRegD = new JTextField(reg);
        txtDropD = new JTextField(drop);

        txtDay = new JTextField(day);
        txtStart = new JTextField(start);
        txtEnd = new JTextField(end);
        txtRoom = new JTextField(room);
        txtCapacity = new JTextField(String.valueOf(capacity));
        txtSem = new JTextField(semester);
        txtYear = new JTextField(String.valueOf(year));

        int row = 0;
        addField(panel, c, row++, "Code:", txtCode);
        addField(panel, c, row++, "Course Name:", txtName);
        addField(panel, c, row++, "Instructor:", txtInstructor);
        addField(panel, c, row++, "Title:", txtTitle);
        addField(panel, c, row++, "Credits:", txtCredits);
        addField(panel, c, row++, "Reg Deadline:", txtRegD);
        addField(panel, c, row++, "Drop Deadline:", txtDropD);

        addField(panel, c, row++, "Day:", txtDay);
        addField(panel, c, row++, "Start Time:", txtStart);
        addField(panel, c, row++, "End Time:", txtEnd);
        addField(panel, c, row++, "Room:", txtRoom);
        addField(panel, c, row++, "Capacity:", txtCapacity);
        addField(panel, c, row++, "Semester:", txtSem);
        addField(panel, c, row++, "Year:", txtYear);

        JButton save = new JButton("Save");
        save.addActionListener(e -> {
            if (validateInputs()) {
                saveChanges();
            }
        });
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        panel.add(save, c);

        add(panel);
    }

    private void addField(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1;
        p.add(new JLabel(label), c);
        c.gridx = 1;
        p.add(field, c);
    }

    private boolean validateInputs() {

        // Required text fields
        if (txtCode.getText().trim().isEmpty() ||
                txtName.getText().trim().isEmpty() ||
                txtInstructor.getText().trim().isEmpty() ||
                txtTitle.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(this,
                    "Code, Course Name, Instructor, and Title cannot be empty.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Instructor name must NOT contain digits
        if (txtInstructor.getText().matches(".*\\d.*")) {
            JOptionPane.showMessageDialog(this,
                    "Instructor name cannot contain numbers.",
                    "Invalid Instructor",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Credits must be a positive integer
        try {
            int credits = Integer.parseInt(txtCredits.getText());
            if (credits <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Credits must be a positive number.",
                        "Invalid Credits",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Credits must be a valid number.",
                    "Invalid Credits",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Capacity must be a positive integer
        try {
            int cap = Integer.parseInt(txtCapacity.getText());
            if (cap <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Capacity must be greater than 0.",
                        "Invalid Capacity",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Capacity must be a valid number.",
                    "Invalid Capacity",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Day must be one of MON/TUE/WED...
        String day = txtDay.getText().trim().toUpperCase();
        String[] validDays = {"MON","TUE","WED","THU","FRI","SAT","SUN"};

        boolean matches = false;
        for (String d : validDays) if (d.equals(day)) matches = true;

        if (!matches) {
            JOptionPane.showMessageDialog(this,
                    "Day must be one of: MON, TUE, WED, THU, FRI, SAT, SUN.",
                    "Invalid Day",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Time format validation: hh:mm:ss
        if (!txtStart.getText().matches("\\d{2}:\\d{2}:\\d{2}") ||
                !txtEnd.getText().matches("\\d{2}:\\d{2}:\\d{2}")) {

            JOptionPane.showMessageDialog(this,
                    "Time must be in the format HH:MM:SS (e.g. 09:00:00).",
                    "Invalid Time",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Semester must follow pattern YYYYX (example: 2025S)
        if (!txtSem.getText().matches("\\d{4}[A-Z]")) {
            JOptionPane.showMessageDialog(this,
                    "Semester must have format like 2025S, 2025F, etc.",
                    "Invalid Semester",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Year must be a valid integer >= 2000
        try {
            int year = Integer.parseInt(txtYear.getText());
            if (year < 2000) {
                JOptionPane.showMessageDialog(this,
                        "Year must be 2000 or later.",
                        "Invalid Year",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Year must be a valid number.",
                    "Invalid Year",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true; // If everything passes
    }

    private void saveChanges() {
        try (Connection conn = DBConnection.getERPConnection()) {

            String sqlCourse = """
                UPDATE courses SET 
                code=?, course_name=?, instructor=?, title=?, credits=?, 
                reg_deadline=?, drop_deadline=? 
                WHERE course_id=?
            """;

            PreparedStatement ps = conn.prepareStatement(sqlCourse);
            ps.setString(1, txtCode.getText());
            ps.setString(2, txtName.getText());
            ps.setString(3, txtInstructor.getText());
            ps.setString(4, txtTitle.getText());
            ps.setInt(5, Integer.parseInt(txtCredits.getText()));
            ps.setString(6, txtRegD.getText());
            ps.setString(7, txtDropD.getText());
            ps.setInt(8, courseId);
            ps.executeUpdate();

            String sqlTT = """
                UPDATE timetable SET 
                day=?, start_time=?, end_time=?, room=?, capacity=?, 
                semester=?, year=? 
                WHERE id=?
            """;

            PreparedStatement ps2 = conn.prepareStatement(sqlTT);
            ps2.setString(1, txtDay.getText());
            ps2.setString(2, txtStart.getText());
            ps2.setString(3, txtEnd.getText());
            ps2.setString(4, txtRoom.getText());
            ps2.setInt(5, Integer.parseInt(txtCapacity.getText()));
            ps2.setString(6, txtSem.getText());
            ps2.setInt(7, Integer.parseInt(txtYear.getText()));
            ps2.setInt(8, sectionId);
            ps2.executeUpdate();

            parent.loadCourses();
            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
