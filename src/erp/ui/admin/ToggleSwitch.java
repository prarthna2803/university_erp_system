package erp.ui.admin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ToggleSwitch extends JComponent {

    private boolean isOn = false;
    private final List<SwitchListener> listeners = new ArrayList<>();

    // DIMENSIONS (Final iOS style)
    private static final int WIDTH = 50;
    private static final int HEIGHT = 28;
    private static final int KNOB_SIZE = 24;

    public ToggleSwitch() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setMaximumSize(new Dimension(WIDTH, HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggle();
            }
        });
    }

    public void setState(boolean on) {
        this.isOn = on;
        repaint();
    }

    public boolean isOn() {
        return isOn;
    }

    public void addSwitchListener(SwitchListener l) {
        listeners.add(l);
    }

    private void toggle() {
        isOn = !isOn;
        for (SwitchListener l : listeners) {
            l.onSwitch(isOn);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background color
        g2.setColor(isOn ? new Color(0x34C759) : new Color(0xCCCCCC));
        g2.fillRoundRect(0, 0, WIDTH, HEIGHT, HEIGHT, HEIGHT);

        // Thumb position
        int knobX = isOn ? (WIDTH - KNOB_SIZE - 2) : 2;

        // Draw thumb
        g2.setColor(Color.WHITE);
        g2.fillOval(knobX, 2, KNOB_SIZE, KNOB_SIZE);

        g2.dispose();
    }

    // Listener interface
    public interface SwitchListener {
        void onSwitch(boolean newState);
    }
}
