import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class LoginFrame extends JFrame {
    // ── Dark Palette (matching entire application suite) ──
    private static final Color BG_APP      = new Color(15,  17,  23);
    private static final Color BG_CARD     = new Color(26,  29,  46);
    private static final Color BG_CARD2    = new Color(37,  40,  64);
    private static final Color ACCENT      = new Color(77,  142, 247);
    private static final Color ACCENT_HOVER= new Color(54,  116, 207);
    private static final Color TEXT_PRI    = new Color(226, 232, 240);
    private static final Color TEXT_MUT    = new Color(148, 163, 184);
    private static final Color BORDER      = new Color(45,  49,  84);
    private static final Color DANGER      = new Color(239, 68,  68);
    private static final Color SUCCESS     = new Color(34,  197, 94);

    private static final Font UI        = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_MEDIUM = new Font("Segoe UI", Font.BOLD, 12);

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JComboBox<String> roleBox =
            new JComboBox<>(new String[]{"Student", "Admin", "Faculty"});
    private final JButton loginButton = new JButton("Sign in");
    private final JLabel statusLabel = new JLabel(" ");

    public LoginFrame() {
        super("AssessPro | Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setSize(1100, 720);
        setLocationRelativeTo(null);
        UIUtils.setWindowIcon(this);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e -> performLogin());
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);

        // --- Left Brand Banner Panel ---
        JPanel brandPanel = new JPanel();
        brandPanel.setBackground(new Color(10, 12, 18));
        brandPanel.setPreferredSize(new Dimension(420, 0));
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setBorder(new EmptyBorder(48, 48, 36, 48));

        // Brand Row
        JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        brandRow.setOpaque(false);
        brandRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel mark = UIUtils.createLogoLabel(46);
        brandRow.add(mark);
        brandRow.add(Box.createHorizontalStrut(14));

        JPanel brandWords = new JPanel();
        brandWords.setOpaque(false);
        brandWords.setLayout(new BoxLayout(brandWords, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("ASSESSPRO");
        brand.setForeground(TEXT_PRI);
        brand.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JLabel brandSub = new JLabel("ASSESSMENT PLATFORM");
        brandSub.setForeground(ACCENT);
        brandSub.setFont(new Font("Segoe UI", Font.BOLD, 9));
        brandWords.add(brand);
        brandWords.add(Box.createVerticalStrut(3));
        brandWords.add(brandSub);
        brandRow.add(brandWords);

        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER);
        separator.setBackground(BORDER);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JLabel eyebrow = new JLabel("A MORE FOCUSED WAY TO ASSESS");
        eyebrow.setForeground(TEXT_MUT);
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 10));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel headline = new JLabel("<html>Every result<br>has a story.</html>");
        headline.setForeground(TEXT_PRI);
        headline.setFont(new Font("Segoe UI", Font.BOLD, 36));
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
            "<html><div style='width:280px; line-height:1.6'>" +
            "Bring examinations, student performance and academic records " +
            "together in one clear, structured workspace." +
            "</div></html>");
        description.setForeground(TEXT_MUT);
        description.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel featurePanel = new JPanel();
        featurePanel.setOpaque(false);
        featurePanel.setLayout(new BoxLayout(featurePanel, BoxLayout.Y_AXIS));
        featurePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        featurePanel.add(featureRow("01", "Structured assessments"));
        featurePanel.add(Box.createVerticalStrut(12));
        featurePanel.add(featureRow("02", "Reliable result records"));
        featurePanel.add(Box.createVerticalStrut(12));
        featurePanel.add(featureRow("03", "Clear performance insights"));

        JLabel footer = new JLabel("AssessPro v2.0 • Secured Exam Suite");
        footer.setForeground(new Color(90, 105, 125));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandPanel.add(brandRow);
        brandPanel.add(Box.createVerticalStrut(28));
        brandPanel.add(separator);
        brandPanel.add(Box.createVerticalStrut(32));
        brandPanel.add(eyebrow);
        brandPanel.add(Box.createVerticalStrut(14));
        brandPanel.add(headline);
        brandPanel.add(Box.createVerticalStrut(16));
        brandPanel.add(description);
        brandPanel.add(Box.createVerticalStrut(32));
        brandPanel.add(featurePanel);
        brandPanel.add(Box.createVerticalGlue());
        brandPanel.add(footer);

        // --- Right Container with ScrollPane for Small Windows / Responsiveness ---
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(BG_APP);

        JPanel form = new JPanel();
        form.setBackground(BG_CARD);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(38, 42, 34, 42)
        ));
        form.setPreferredSize(new Dimension(420, 560));
        form.setMinimumSize(new Dimension(380, 530));
        form.setMaximumSize(new Dimension(460, 600));

        JLabel eyebrowForm = new JLabel("ACCOUNT ACCESS");
        eyebrowForm.setForeground(TEXT_MUT);
        eyebrowForm.setFont(new Font("Segoe UI", Font.BOLD, 10));
        eyebrowForm.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Welcome back");
        title.setForeground(TEXT_PRI);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in with your AssessPro credentials.");
        subtitle.setForeground(TEXT_MUT);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleField(usernameField);
        styleField(passwordField);
        styleCombo(roleBox);

        form.add(eyebrowForm);
        form.add(Box.createVerticalStrut(8));
        form.add(title);
        form.add(Box.createVerticalStrut(6));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(26));

        form.add(fieldLabel("USERNAME"));
        form.add(Box.createVerticalStrut(7));
        form.add(iconField(usernameField, IconType.USER));
        form.add(Box.createVerticalStrut(18));

        form.add(fieldLabel("PASSWORD"));
        form.add(Box.createVerticalStrut(7));
        form.add(iconField(passwordField, IconType.LOCK));
        form.add(Box.createVerticalStrut(18));

        form.add(fieldLabel("SIGN IN AS"));
        form.add(Box.createVerticalStrut(7));
        form.add(iconCombo(roleBox, IconType.ROLE));
        form.add(Box.createVerticalStrut(24));

        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.setPreferredSize(new Dimension(336, 44));
        loginButton.setBackground(ACCENT);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginButton.setFocusPainted(false);
        loginButton.setBorder(new EmptyBorder(10, 16, 10, 16));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(ACCENT_HOVER);
            }
            @Override public void mouseExited(MouseEvent e) {
                loginButton.setBackground(ACCENT);
            }
        });
        form.add(loginButton);

        statusLabel.setForeground(DANGER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(10));
        form.add(statusLabel);

        // Register link (mirrors JavaFX "New Student? Register Here")
        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        registerRow.setOpaque(false);
        registerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel registerPrompt = new JLabel("New Student?");
        registerPrompt.setForeground(TEXT_MUT);
        registerPrompt.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton registerBtn = new JButton("Register Here");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        registerBtn.setForeground(ACCENT);
        registerBtn.setBorderPainted(false);
        registerBtn.setContentAreaFilled(false);
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerBtn.setBorder(new EmptyBorder(0, 0, 0, 0));
        registerBtn.addActionListener(e -> {
            new StudentRegistrationFrame(LoginFrame.this).setVisible(true);
        });
        registerRow.add(registerPrompt);
        registerRow.add(registerBtn);

        JSeparator formSeparator = new JSeparator();
        formSeparator.setForeground(BORDER);
        formSeparator.setBackground(BORDER);
        formSeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        formSeparator.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(Box.createVerticalStrut(6));
        form.add(registerRow);
        form.add(Box.createVerticalStrut(14));
        form.add(formSeparator);
        form.add(Box.createVerticalStrut(12));

        JLabel note = new JLabel("Access is managed by your institution.");
        note.setForeground(TEXT_MUT);
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(note);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        right.add(form, gbc);

        JScrollPane rightScroll = new JScrollPane(right);
        rightScroll.setBorder(null);
        rightScroll.setBackground(BG_APP);
        rightScroll.getViewport().setBackground(BG_APP);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        root.add(brandPanel, BorderLayout.WEST);
        root.add(rightScroll, BorderLayout.CENTER);
        return root;
    }

    private JPanel featureRow(String number, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        JLabel num = new JLabel(number);
        num.setForeground(ACCENT);
        num.setFont(new Font("Segoe UI", Font.BOLD, 10));
        num.setPreferredSize(new Dimension(30, 18));
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRI);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        row.add(num);
        row.add(label);
        return row;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MUT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private enum IconType { USER, LOCK, ROLE }

    private JPanel iconField(JComponent field, IconType type) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setBackground(BG_CARD2);
        wrapper.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(0, 12, 0, 12)
        ));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapper.setPreferredSize(new Dimension(336, 44));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        VectorIcon icon = new VectorIcon(type);
        field.setBorder(null);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRI);
        field.setBackground(BG_CARD2);

        wrapper.add(icon, BorderLayout.WEST);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel iconCombo(JComboBox<String> combo, IconType type) {
        JPanel wrapper = new JPanel(new BorderLayout(10, 0));
        wrapper.setBackground(BG_CARD2);
        wrapper.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(0, 12, 0, 10)
        ));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapper.setPreferredSize(new Dimension(336, 44));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        VectorIcon icon = new VectorIcon(type);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setForeground(TEXT_PRI);
        combo.setBackground(BG_CARD2);

        wrapper.add(icon, BorderLayout.WEST);
        wrapper.add(combo, BorderLayout.CENTER);
        return wrapper;
    }

    private void styleField(JTextField field) {
        field.setFont(UI);
        field.setForeground(TEXT_PRI);
        field.setBackground(BG_CARD2);
        field.setCaretColor(TEXT_PRI);
        field.setBorder(null);
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setFont(UI);
        combo.setBackground(BG_CARD2);
        combo.setForeground(TEXT_PRI);
        combo.setBorder(null);
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
        @Override
        protected JButton createArrowButton() {   // flat arrow, no box
            JButton b = new JButton("\u25BE");
            b.setForeground(TEXT_MUT);
            b.setBorder(null);
            b.setContentAreaFilled(false);
            b.setFocusable(false);
            return b;
        }
        });
    }


    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter both username and password.");
            return;
        }

        loginButton.setEnabled(false);
        statusLabel.setForeground(TEXT_MUT);
        statusLabel.setText("Checking credentials...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected String doInBackground() {
                try {
                    return new LoginService().authenticate(username, password, role);
                } catch (Exception ex) {
                    failure = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    String authenticatedUser = get();
                    if (failure != null) {
                        statusLabel.setForeground(DANGER);
                        statusLabel.setText("Database connection failed. Check MySQL settings.");
                        failure.printStackTrace();
                    } else if (authenticatedUser == null) {
                        statusLabel.setForeground(DANGER);
                        statusLabel.setText("Invalid username, password or account type.");
                    } else {
                        statusLabel.setForeground(SUCCESS);
                        statusLabel.setText("Signed in successfully.");
                        dispose();
                        if ("Student".equalsIgnoreCase(role)) {
                            new StudentDashboardFrame(authenticatedUser).setVisible(true);
                        } else if ("Admin".equalsIgnoreCase(role)) {
                            new AdminDashboardFrame(authenticatedUser).setVisible(true);
                        } else if ("Faculty".equalsIgnoreCase(role)) {
                            new FacultyDashboardFrame(authenticatedUser).setVisible(true);
                        }
                    }
                } catch (Exception ex) {
                    statusLabel.setForeground(DANGER);
                    statusLabel.setText("Unable to complete sign in.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Crisp, scalable vector icons rendered in Graphics2D for User, Lock, and Role.
     */
    private static class VectorIcon extends JComponent {
        private final IconType type;

        VectorIcon(IconType type) {
            this.type = type;
            setPreferredSize(new Dimension(18, 18));
            setMinimumSize(new Dimension(18, 18));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;

            g.setColor(TEXT_MUT);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case USER:
                    // Head
                    g.drawOval(cx - 4, cy - 8, 8, 8);
                    // Shoulders arc
                    g.drawArc(cx - 7, cy + 1, 14, 11, 25, 130);
                    break;
                case LOCK:
                    // Body
                    g.drawRoundRect(cx - 6, cy - 2, 12, 10, 3, 3);
                    // Shackle
                    g.drawArc(cx - 4, cy - 8, 8, 9, 0, 180);
                    // Keyhole dot
                    g.fillOval(cx - 1, cy + 2, 2, 3);
                    break;
                case ROLE:
                    // Shield / badge outline
                    int[] xPoints = { cx, cx + 6, cx + 6, cx, cx - 6, cx - 6 };
                    int[] yPoints = { cy - 7, cy - 5, cy + 2, cy + 7, cy + 2, cy - 5 };
                    g.drawPolygon(xPoints, yPoints, 6);
                    // Subtle center star/check dot
                    g.fillOval(cx - 1, cy - 1, 3, 3);
                    break;
            }

            g.dispose();
        }
    }
}
