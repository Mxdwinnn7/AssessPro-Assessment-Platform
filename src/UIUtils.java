import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;

/**
 * Utility class for managing application-wide icon and window branding
 * across all AssessPro Swing screens.
 */
public final class UIUtils {
    private static Image cachedIcon = null;

    private UIUtils() {}

    /**
     * Loads the master AssessPro application icon.
     */
    public static synchronized Image getAppIcon() {
        if (cachedIcon != null) {
            return cachedIcon;
        }

        // 1. Try file path in current working directory
        String[] candidatePaths = {
            "resources/assesspro-icon.png",
            "../AssessPro-Swing/resources/assesspro-icon.png",
            "AssessPro-Swing/resources/assesspro-icon.png"
        };

        for (String path : candidatePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    cachedIcon = ImageIO.read(file);
                    if (cachedIcon != null) {
                        return cachedIcon;
                    }
                } catch (Exception ignored) {}
            }
        }

        // 2. Try classpath resource
        String[] candidateResources = {
            "/resources/assesspro-icon.png",
            "/assesspro-icon.png",
            "resources/assesspro-icon.png",
            "assesspro-icon.png"
        };

        for (String res : candidateResources) {
            try (InputStream in = UIUtils.class.getResourceAsStream(res)) {
                if (in != null) {
                    cachedIcon = ImageIO.read(in);
                    if (cachedIcon != null) {
                        return cachedIcon;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 3. Fallback: generate a styled placeholder icon programmatically
        cachedIcon = createFallbackIcon(256);
        return cachedIcon;
    }

    /**
     * Applies the AssessPro icon to any Window (JFrame, JDialog, etc.)
     */
    public static void setWindowIcon(Window window) {
        if (window == null) return;
        Image icon = getAppIcon();
        if (icon != null) {
            window.setIconImage(icon);
        }
    }

    // ── Centralized Dark Theme Input Styling ─────────────────────────────────

    public static final Color BG_FIELD     = new Color(37, 40, 64);   // #252840
    public static final Color BORDER_FIELD = new Color(55, 62, 95);   // #373E5F
    public static final Color BORDER_FOCUS = new Color(77, 142, 247);  // #4D8EF7 Accent
    public static final Color TEXT_FIELD   = new Color(226, 232, 240); // #E2E8F0
    public static final Color TEXT_SEL_BG  = new Color(60, 80, 130);

    /**
     * Styles any JTextField or JPasswordField with high-contrast text, proper padding,
     * caret color, selection colors, and an interactive focus glow.
     */
    public static void styleTextField(javax.swing.text.JTextComponent field, int topPad, int leftPad, int bottomPad, int rightPad) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_FIELD);
        field.setBackground(BG_FIELD);
        field.setCaretColor(TEXT_FIELD);
        field.setSelectionColor(TEXT_SEL_BG);
        field.setSelectedTextColor(Color.WHITE);

        javax.swing.border.Border idleBorder = new javax.swing.border.CompoundBorder(
            new javax.swing.border.LineBorder(BORDER_FIELD, 1, true),
            new javax.swing.border.EmptyBorder(topPad, leftPad, bottomPad, rightPad)
        );
        javax.swing.border.Border focusBorder = new javax.swing.border.CompoundBorder(
            new javax.swing.border.LineBorder(BORDER_FOCUS, 1, true),
            new javax.swing.border.EmptyBorder(topPad, leftPad, bottomPad, rightPad)
        );

        field.setBorder(idleBorder);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.setBorder(focusBorder);
                field.repaint();
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.setBorder(idleBorder);
                field.repaint();
            }
        });
    }

    public static void styleTextField(javax.swing.text.JTextComponent field) {
        styleTextField(field, 8, 12, 8, 12);
    }

    /**
     * Returns a scaled ImageIcon ready to use in Swing components.
     */
    public static ImageIcon getScaledIcon(int width, int height) {
        Image img = getAppIcon();
        if (img == null) return null;
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Returns a ready-to-use JLabel displaying the AssessPro brand icon at the specified size.
     */
    public static JLabel createLogoLabel(int size) {
        ImageIcon icon = getScaledIcon(size, size);
        JLabel label = new JLabel(icon);
        label.setPreferredSize(new Dimension(size, size));
        label.setMaximumSize(new Dimension(size, size));
        label.setOpaque(false);
        return label;
    }

    private static BufferedImage createFallbackIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(28, 31, 34));
        g.fillRoundRect(0, 0, size, size, size / 4, size / 4);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.4)));
        FontMetrics fm = g.getFontMetrics();
        String text = "AP";
        int x = (size - fm.stringWidth(text)) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        g.dispose();
        return img;
    }
}
