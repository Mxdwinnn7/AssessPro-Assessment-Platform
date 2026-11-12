import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;

/**
 * Shared dark SaaS widgets for the student, admin, and faculty dashboards.
 */
public final class DashboardUI {
    public static final Color BG_APP        = new Color(15, 17, 23);
    public static final Color BG_CARD       = new Color(26, 29, 46);
    public static final Color BG_CARD_HOVER = new Color(35, 39, 61);
    public static final Color BG_CARD2      = new Color(37, 40, 64);
    public static final Color ACCENT        = new Color(77, 142, 247);
    public static final Color EMERALD       = new Color(34, 197, 94);
    public static final Color AMBER         = new Color(245, 158, 11);
    public static final Color DANGER        = new Color(239, 68, 68);
    public static final Color TEXT_PRI      = new Color(226, 232, 240);
    public static final Color TEXT_MUT      = new Color(148, 163, 184);
    public static final Color BORDER        = new Color(45, 49, 84);
    public static final Color GOLD          = new Color(234, 179, 8);
    public static final Color SILVER        = new Color(186, 198, 216);
    public static final Color BRONZE        = new Color(205, 127, 50);

    public static final Font UI      = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font SECTION = new Font("Segoe UI", Font.BOLD, 18);

    private DashboardUI() {}

    /** Letter from the same percentage the result screen already computes. */
    public static String letterGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 80) return "B";
        if (percentage >= 70) return "C";
        if (percentage >= 60) return "D";
        return "F";
    }

    public static String formatRemaining(Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        long days = minutes / (60 * 24);
        long hours = (minutes % (60 * 24)) / 60;
        long mins = minutes % 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return Math.max(1, mins) + "m";
    }

    public static JScrollPane scroll(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        scroll.getViewport().setBackground(BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    public static JPanel header(String portal, Runnable onLogout) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(14, 30, 14, 30)));

        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT);
        JLabel eyebrow = new JLabel(portal);
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 10));
        eyebrow.setForeground(TEXT_MUT);

        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logo);
        branding.add(eyebrow);

        QuietButton logout = new QuietButton("Logout");
        logout.addActionListener(e -> onLogout.run());
        header.add(branding, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SECTION);
        label.setForeground(TEXT_PRI);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UI);
        label.setForeground(TEXT_MUT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static JLabel badge(String text, Color color) {
        JLabel label = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 36));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
                g2.drawRoundRect(0, 0, getWidth() - 2, getHeight() - 2, getHeight() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setOpaque(false);
        label.setForeground(color);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setBorder(new EmptyBorder(3, 10, 3, 10));
        return label;
    }

    public static JComponent iconWell(Glyph glyph, Color color) {
        JPanel well = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 36));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        well.setOpaque(false);
        well.setPreferredSize(new Dimension(42, 42));
        well.setMinimumSize(new Dimension(42, 42));
        well.setMaximumSize(new Dimension(42, 42));
        well.add(new JLabel(new GlyphIcon(glyph, color)));
        return well;
    }

    public enum Glyph { CLIPBOARD, DATABASE, SLIDERS, USERS, CHART, LOG }

    public static final class GlyphIcon implements Icon {
        private final Glyph glyph;
        private final Color color;

        public GlyphIcon(Glyph glyph, Color color) {
            this.glyph = glyph;
            this.color = color;
        }

        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (glyph) {
                case CLIPBOARD -> paintClipboard(g2);
                case DATABASE -> paintDatabase(g2);
                case SLIDERS -> paintSliders(g2);
                case USERS -> paintUsers(g2);
                case CHART -> paintChart(g2);
                case LOG -> paintLog(g2);
                default -> { }
            }
            g2.dispose();
        }

        private static void paintClipboard(Graphics2D g2) {
            g2.drawRoundRect(5, 4, 12, 15, 3, 3);
            g2.drawRoundRect(8, 2, 6, 4, 2, 2);
            g2.drawLine(8, 10, 14, 10);
            g2.drawLine(8, 13, 14, 13);
            g2.drawLine(8, 16, 12, 16);
        }

        private static void paintDatabase(Graphics2D g2) {
            g2.drawOval(4, 3, 14, 5);
            g2.drawLine(4, 6, 4, 16);
            g2.drawLine(18, 6, 18, 16);
            g2.drawArc(4, 13, 14, 5, 180, 180);
            g2.drawArc(4, 8, 14, 5, 180, 180);
        }

        private static void paintSliders(Graphics2D g2) {
            g2.drawLine(3, 6, 19, 6);
            g2.drawLine(3, 11, 19, 11);
            g2.drawLine(3, 16, 19, 16);
            g2.fillOval(7, 4, 4, 4);
            g2.fillOval(12, 9, 4, 4);
            g2.fillOval(6, 14, 4, 4);
        }

        private static void paintChart(Graphics2D g2) {
            g2.drawLine(3, 3, 3, 18);
            g2.drawLine(3, 18, 19, 18);
            g2.drawRoundRect(6, 11, 3, 7, 1, 1);
            g2.drawRoundRect(11, 7, 3, 11, 1, 1);
            g2.drawRoundRect(16, 4, 3, 14, 1, 1);
        }

        private static void paintLog(Graphics2D g2) {
            g2.drawRoundRect(4, 3, 14, 16, 3, 3);
            g2.fillOval(7, 7, 2, 2);
            g2.fillOval(7, 11, 2, 2);
            g2.fillOval(7, 15, 2, 2);
            g2.drawLine(11, 8, 15, 8);
            g2.drawLine(11, 12, 15, 12);
            g2.drawLine(11, 16, 15, 16);
        }

        private static void paintUsers(Graphics2D g2) {
            g2.drawOval(4, 3, 6, 6);
            g2.drawArc(2, 11, 10, 8, 10, 160);
            g2.drawOval(12, 4, 5, 5);
            g2.drawArc(11, 11, 8, 7, 20, 140);
        }
    }

    /** Vertical stack that does not stretch to fill leftover scroll space. */
    public static class Stack extends JPanel {
        public Stack() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension preferred = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, preferred.height);
        }
    }

    public static class RoundedCard extends JPanel {
        private Color accent;
        private boolean hoverEnabled;
        private boolean hovered;
        private Runnable onClick;
        private boolean bound;

        public RoundedCard() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(16, 18, 16, 18));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        public RoundedCard accent(Color accent) {
            this.accent = accent;
            return this;
        }

        public RoundedCard hoverable() {
            this.hoverEnabled = true;
            return this;
        }

        public RoundedCard onClick(Runnable onClick) {
            this.onClick = onClick;
            this.hoverEnabled = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return this;
        }

        public void bindInteraction() {
            if (bound) return;
            bound = true;
            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!hoverEnabled) return;
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!hoverEnabled) return;
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), RoundedCard.this);
                    if (!contains(p)) {
                        hovered = false;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClick != null && SwingUtilities.isLeftMouseButton(e)) onClick.run();
                }
            };
            attach(this, adapter);
        }

        private void attach(Component component, MouseAdapter adapter) {
            component.addMouseListener(adapter);
            if (onClick != null) {
                component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) {
                    attach(child, adapter);
                }
            }
        }

        @Override
        public Dimension getMaximumSize() {
            Dimension preferred = getPreferredSize();
            return new Dimension(Integer.MAX_VALUE, preferred.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 1;
            int y = 1;
            int w = Math.max(1, getWidth() - 3);
            int h = Math.max(1, getHeight() - 4);
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(x, y + 3, w, h, 18, 18);
            g2.setColor(hovered ? BG_CARD_HOVER : BG_CARD);
            g2.fillRoundRect(x, y, w, h, 18, 18);
            if (accent != null) {
                Shape previous = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(x, y, w, h, 18, 18));
                g2.setColor(accent);
                g2.fillRect(x, y, w, 3);
                g2.setClip(previous);
            }
            g2.setColor(BORDER);
            g2.drawRoundRect(x, y, w, h, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class ProgressTrack extends JComponent {
        private final int percent;
        private final Color fill;

        public ProgressTrack(int percent, Color fill) {
            this.percent = Math.max(0, Math.min(100, percent));
            this.fill = fill;
            setOpaque(false);
            setPreferredSize(new Dimension(220, 8));
            setMinimumSize(new Dimension(80, 8));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 8);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            g2.setColor(BG_CARD2);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            int filled = (int) Math.round(getWidth() * (percent / 100.0));
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, Math.max(h, filled), h, h, h);
            g2.dispose();
        }
    }

    public static class PillButton extends JButton {
        private boolean active;

        public PillButton(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(7, 14, 7, 14));
            setRolloverEnabled(true);
            setForeground(active ? Color.WHITE : TEXT_PRI);
        }

        public void setActive(boolean active) {
            this.active = active;
            setForeground(active ? Color.WHITE : TEXT_PRI);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setColor(ACCENT);
            } else if (getModel().isRollover()) {
                g2.setColor(BG_CARD_HOVER);
            } else {
                g2.setColor(BG_CARD2);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            if (!active) {
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static class PrimaryButton extends JButton {
        public PrimaryButton(String text) {
            super(text);
            setFont(UI_BOLD);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) g2.setColor(BG_CARD2);
            else if (getModel().isRollover()) g2.setColor(new Color(96, 158, 250));
            else g2.setColor(ACCENT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            setForeground(isEnabled() ? Color.WHITE : TEXT_MUT);
            super.paintComponent(g);
        }
    }

    public static class QuietButton extends JButton {
        public QuietButton(String text) {
            super(text);
            setFont(UI_BOLD);
            setForeground(TEXT_PRI);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 16, 8, 16));
            setRolloverEnabled(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isRollover() ? BG_CARD_HOVER : BG_CARD2);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
