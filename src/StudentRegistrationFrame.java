import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Student Registration window — dark themed.
 */
public class StudentRegistrationFrame extends JDialog {

    // ── Dark Palette ──────────────────────────────────────────────────────────
    private static final Color BG_APP   = new Color(15,  17,  23);
    private static final Color BG_CARD  = new Color(26,  29,  46);
    private static final Color BG_CARD2 = new Color(37,  40,  64);
    private static final Color ACCENT   = new Color(77,  142, 247);
    private static final Color TEXT_PRI = new Color(226, 232, 240);
    private static final Color TEXT_MUT = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(45,  49,  84);
    private static final Color SUCCESS  = new Color(34,  197, 94);
    private static final Color DANGER   = new Color(239, 68,  68);

    private final JTextField     nameField     = new JTextField();
    private final JTextField     usernameField = new JTextField();
    private final JPasswordField passField     = new JPasswordField();
    private final JPasswordField confirmField  = new JPasswordField();
    private final JLabel         statusLabel   = new JLabel(" ");
    private final JButton        createButton  = new JButton("CREATE ACCOUNT");

    public StudentRegistrationFrame(Frame owner) {
        super(owner, "AssessPro – Student Registration", true);
        setSize(520, 580);
        setMinimumSize(new Dimension(460, 540));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        UIUtils.setWindowIcon(this);
        setContentPane(buildContent());
        getRootPane().setDefaultButton(createButton);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(16, 24, 16, 24)
        ));
        JLabel logoLabel = new JLabel("AssessPro");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        logoLabel.setForeground(ACCENT);
        JLabel roleLabel = new JLabel("STUDENT REGISTRATION");
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        roleLabel.setForeground(TEXT_MUT);
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logoLabel);
        branding.add(roleLabel);
        header.add(branding, BorderLayout.WEST);

        // Form
        JPanel form = new JPanel();
        form.setBackground(BG_APP);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(28, 32, 24, 32));

        JLabel title = new JLabel("Create Student Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Enter your details to get started.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(title);
        form.add(Box.createVerticalStrut(6));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(22));

        form.add(fieldLabel("FULL NAME"));
        form.add(Box.createVerticalStrut(6));
        form.add(styledField(nameField));
        form.add(Box.createVerticalStrut(14));

        form.add(fieldLabel("USERNAME"));
        form.add(Box.createVerticalStrut(6));
        form.add(styledField(usernameField));
        form.add(Box.createVerticalStrut(14));

        form.add(fieldLabel("PASSWORD"));
        form.add(Box.createVerticalStrut(6));
        form.add(styledField(passField));
        form.add(Box.createVerticalStrut(14));

        form.add(fieldLabel("CONFIRM PASSWORD"));
        form.add(Box.createVerticalStrut(6));
        form.add(styledField(confirmField));
        form.add(Box.createVerticalStrut(20));

        styleButton(createButton, ACCENT, Color.WHITE);
        createButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        createButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        createButton.addActionListener(e -> createAccount());
        form.add(createButton);

        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(Box.createVerticalStrut(8));
        form.add(statusLabel);

        form.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setBackground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(sep);
        form.add(Box.createVerticalStrut(12));

        JLabel loginPrompt = new JLabel("Already have an account?");
        loginPrompt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loginPrompt.setForeground(TEXT_MUT);
        loginPrompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton loginLink = linkButton("Sign in");
        loginLink.addActionListener(e -> dispose());

        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        loginRow.setOpaque(false);
        loginRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginRow.add(loginPrompt);
        loginRow.add(loginLink);
        form.add(loginRow);

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        return root;
    }

    private void createAccount() {
        String fullName = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passField.getPassword());
        String confirm  = new String(confirmField.getPassword());

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            setStatus("Please fill in all fields.", false);
            return;
        }
        if (!password.equals(confirm)) {
            setStatus("Passwords do not match.", false);
            return;
        }
        if (password.length() < 6) {
            setStatus("Password must be at least 6 characters.", false);
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, 'Student')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.executeUpdate();
            showSuccessScreen(fullName, username);
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                setStatus("Username already exists. Choose another.", false);
            } else {
                setStatus("Registration failed: " + ex.getMessage(), false);
            }
        }
    }

    private void showSuccessScreen(String fullName, String username) {
        JDialog successDialog = new JDialog(this, "Registration Successful", true);
        successDialog.setSize(420, 360);
        successDialog.setLocationRelativeTo(this);
        successDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        UIUtils.setWindowIcon(successDialog);

        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(36, 40, 36, 40));

        JLabel tick = new JLabel("✔");
        tick.setFont(new Font("Segoe UI", Font.BOLD, 52));
        tick.setForeground(SUCCESS);
        tick.setHorizontalAlignment(SwingConstants.CENTER);
        tick.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel heading = new JLabel("Student Account Created!");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 20));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msg = new JLabel("<html><div style='text-align:center'>Welcome, <b>" + fullName + "</b>!<br>Your account has been saved successfully.</div></html>");
        msg.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg.setForeground(TEXT_MUT);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton goBtn = new JButton("GO TO LOGIN");
        styleButton(goBtn, ACCENT, Color.WHITE);
        goBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        goBtn.setMaximumSize(new Dimension(220, 42));
        goBtn.addActionListener(e -> {
            successDialog.dispose();
            dispose();
        });

        panel.add(tick);
        panel.add(Box.createVerticalStrut(18));
        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        panel.add(msg);
        panel.add(Box.createVerticalStrut(22));
        panel.add(goBtn);

        successDialog.setContentPane(panel);
        successDialog.setVisible(true);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JComponent styledField(JComponent field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRI);
        field.setBackground(BG_CARD2);
        if (field instanceof JTextField tf) {
            tf.setCaretColor(TEXT_PRI);
            tf.setBorder(null);
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_CARD2);
        wrapper.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(0, 12, 0, 12)
        ));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBorder(null);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(TEXT_MUT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 18, 10, 18));
    }

    private JButton linkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(ACCENT);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        return btn;
    }

    private void setStatus(String message, Boolean success) {
        statusLabel.setText(message);
        if (success == null) {
            statusLabel.setForeground(TEXT_MUT);
        } else if (success) {
            statusLabel.setForeground(SUCCESS);
        } else {
            statusLabel.setForeground(DANGER);
        }
    }
}
