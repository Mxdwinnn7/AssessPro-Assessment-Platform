import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

/**
 * ToastNotification — Non-blocking, animated floating toast alert for Swing.
 * Uses high-DPI vector Graphics2D rendering for icons to avoid missing Unicode/glyph boxes.
 */
public class ToastNotification extends JPanel {

    public enum Type {
        SUCCESS(new Color(34, 197, 94)),   // Green
        WARNING(new Color(245, 158, 11)),  // Amber
        ERROR(new Color(239, 68, 68)),     // Red
        INFO(new Color(77, 142, 247));     // Blue

        final Color color;

        Type(Color color) {
            this.color = color;
        }
    }

    private static final Color BG_TOAST     = new Color(26, 29, 46);
    private static final Color BORDER_TOAST = new Color(45, 49, 84);
    private static final Color TEXT_PRI     = new Color(226, 232, 240);
    private static final int TOAST_WIDTH    = 340;
    private static final int TOAST_HEIGHT   = 56;
    private static final int MARGIN         = 24;

    private float opacity = 0f;

    public ToastNotification(String message, Type type) {
        setLayout(new BorderLayout(12, 0));
        setOpaque(false);
        setPreferredSize(new Dimension(TOAST_WIDTH, TOAST_HEIGHT));
        setSize(TOAST_WIDTH, TOAST_HEIGHT);

        // Vector icon badge rendered via Graphics2D (guaranteed crisp on any OS/font)
        JComponent iconBadge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

                int w = getWidth();
                int h = getHeight();

                // Translucent circle background
                g2.setColor(new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), 35));
                g2.fillOval(2, 2, w - 4, h - 4);

                // Sharp vector icon stroke
                g2.setColor(type.color);
                g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int cx = w / 2;
                int cy = h / 2;

                switch (type) {
                    case SUCCESS:
                        // Crisp checkmark ✔
                        g2.drawLine(cx - 5, cy, cx - 1, cy + 4);
                        g2.drawLine(cx - 1, cy + 4, cx + 6, cy - 4);
                        break;
                    case ERROR:
                        // Crisp cross ✖
                        g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                        g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                        break;
                    case WARNING:
                        // Warning exclamation mark
                        g2.drawLine(cx, cy - 5, cx, cy + 1);
                        g2.fillRect(cx - 1, cy + 4, 2, 2);
                        break;
                    case INFO:
                        // Info icon
                        g2.drawOval(cx - 6, cy - 6, 12, 12);
                        g2.fillRect(cx - 1, cy - 4, 2, 2);
                        g2.drawLine(cx, cy - 1, cx, cy + 3);
                        break;
                }
                g2.dispose();
            }
        };
        iconBadge.setPreferredSize(new Dimension(36, 36));

        // Message text
        JLabel textLabel = new JLabel("<html><div style='width:240px;'>" + message + "</div></html>");
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        textLabel.setForeground(TEXT_PRI);

        JPanel leftWrapper = new JPanel(new GridBagLayout());
        leftWrapper.setOpaque(false);
        leftWrapper.add(iconBadge);

        add(leftWrapper, BorderLayout.WEST);
        add(textLabel, BorderLayout.CENTER);
        setBorder(new EmptyBorder(8, 14, 8, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));

        // Background
        g2.setColor(BG_TOAST);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

        // Border
        g2.setColor(BORDER_TOAST);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Primary convenience method to show a non-blocking toast.
     */
    public static void show(JFrame frame, String message, Type type) {
        if (frame == null) return;
        SwingUtilities.invokeLater(() -> animate(frame, message, type));
    }

    private static void animate(JFrame frame, String message, Type type) {
        JLayeredPane layeredPane = frame.getLayeredPane();
        if (layeredPane == null) return;

        ToastNotification toast = new ToastNotification(message, type);

        int targetX = frame.getWidth() - TOAST_WIDTH - MARGIN;
        int targetY = frame.getHeight() - TOAST_HEIGHT - MARGIN - 30; // slightly above bottom bar
        int startY  = targetY + 30; // starts 30px lower for slide-up

        toast.setLocation(targetX, startY);
        layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
        layeredPane.revalidate();

        // 1. Slide-in & Fade-in Timer (60 FPS)
        Timer slideInTimer = new Timer(15, null);
        slideInTimer.addActionListener(new ActionListener() {
            int step = 0;
            final int maxSteps = 15;

            @Override
            public void actionPerformed(ActionEvent e) {
                step++;
                float progress = (float) step / maxSteps;
                toast.opacity = Math.min(1.0f, progress);
                int currentY = (int) (startY - (30 * progress));
                toast.setLocation(targetX, currentY);
                toast.repaint();

                if (step >= maxSteps) {
                    slideInTimer.stop();

                    // 2. Display Hold Timer (2.4 seconds)
                    Timer displayTimer = new Timer(2400, ev -> {
                        // 3. Slide-out & Fade-out Timer
                        Timer slideOutTimer = new Timer(15, null);
                        slideOutTimer.addActionListener(new ActionListener() {
                            int outStep = 0;
                            final int maxOut = 12;

                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                outStep++;
                                float outProgress = (float) outStep / maxOut;
                                toast.opacity = Math.max(0.0f, 1.0f - outProgress);
                                int outY = (int) (targetY + (20 * outProgress));
                                toast.setLocation(targetX, outY);
                                toast.repaint();

                                if (outStep >= maxOut) {
                                    slideOutTimer.stop();
                                    layeredPane.remove(toast);
                                    layeredPane.repaint();
                                }
                            }
                        });
                        slideOutTimer.start();
                    });
                    displayTimer.setRepeats(false);
                    displayTimer.start();
                }
            }
        });
        slideInTimer.start();
    }
}
