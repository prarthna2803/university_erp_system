package erp.ui.admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

class AdminCalendarPanel extends JPanel {

    private YearMonth currentMonth = YearMonth.now();
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JPanel grid = new JPanel(new GridLayout(0, 7, 6, 6));

    AdminCalendarPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xF4F7FB));
        setBorder(new EmptyBorder(32, 48, 32, 48));

        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE0E7F1), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel header = new JPanel(new BorderLayout());
        JButton prev = new JButton("<");
        JButton next = new JButton(">");
        prev.addActionListener(e -> { currentMonth = currentMonth.minusMonths(1); refresh(); });
        next.addActionListener(e -> { currentMonth = currentMonth.plusMonths(1); refresh(); });
        monthLabel.setFont(monthLabel.getFont().deriveFont(Font.BOLD, 18f));
        header.add(prev, BorderLayout.WEST);
        header.add(monthLabel, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        grid.setOpaque(false);
        card.add(grid, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
        refresh();
    }

    private void refresh() {
        monthLabel.setText(currentMonth.getMonth() + " " + currentMonth.getYear());
        grid.removeAll();
        for (DayOfWeek day : DayOfWeek.values()) {
            JLabel lbl = new JLabel(day.name().substring(0, 3), SwingConstants.CENTER);
            lbl.setForeground(new Color(0x5A6A85));
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
            grid.add(lbl);
        }

        LocalDate first = currentMonth.atDay(1);
        int leadingBlank = first.getDayOfWeek().getValue() % 7; // Sunday as 0
        for (int i = 0; i < leadingBlank; i++) {
            grid.add(new JLabel(""));
        }
        int length = currentMonth.lengthOfMonth();
        for (int day = 1; day <= length; day++) {
            LocalDate date = currentMonth.atDay(day);
            JLabel lbl = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            lbl.setOpaque(true);
            lbl.setBackground(new Color(0xF8FAFC));
            lbl.setBorder(BorderFactory.createLineBorder(new Color(0xE0E7F1)));
            if (date.equals(LocalDate.now())) {
                lbl.setBackground(new Color(0x3FADA8));
                lbl.setForeground(Color.WHITE);
            }
            grid.add(lbl);
        }
        revalidate();
        repaint();
    }
}

