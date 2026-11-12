import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Create Assessment Screen — dark themed.
 */
public class CreateAssessmentFrame extends JFrame {

    // ── Dark Palette ──────────────────────────────────────────────────────────
    private static final Color BG_APP   = new Color(15,  17,  23);
    private static final Color BG_CARD  = new Color(26,  29,  46);
    private static final Color BG_CARD2 = new Color(37,  40,  64);
    private static final Color ACCENT   = new Color(77,  142, 247);
    private static final Color TEXT_PRI = new Color(226, 232, 240);
    private static final Color TEXT_MUT = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(45,  49,  84);

    private final String adminName;

    private JTextField  titleField;
    private JComboBox<String> subjectBox;
    private JTextArea   descriptionField;
    private JSpinner    durationSpinner;
    private JSpinner    questionSpinner;
    private JCheckBox   randomizeCheckBox;
    private JButton     saveButton;

    public CreateAssessmentFrame(String adminName) {
        super("AssessPro – Create Assessment");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(850, 640));
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);
        setContentPane(root);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(14, 30, 14, 30)
        ));

        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT);
        JLabel adminLabel = new JLabel("ADMIN PANEL");
        adminLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        adminLabel.setForeground(TEXT_MUT);
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logo);
        branding.add(adminLabel);

        JButton backBtn = new JButton("← Dashboard");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backBtn.setForeground(TEXT_PRI);
        backBtn.setBackground(BG_CARD2);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
        backBtn.addActionListener(e -> {
            dispose();
            new AdminDashboardFrame(adminName).setVisible(true);
        });

        header.add(branding, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Content
        JPanel form = new JPanel();
        form.setBackground(BG_APP);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(25, 40, 35, 40));

        JLabel heading = new JLabel("Create Assessment");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Set up your assessment details before adding questions.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(heading);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(20));

        // Details Card
        JPanel detailsCard = new JPanel();
        detailsCard.setBackground(BG_CARD);
        detailsCard.setLayout(new BoxLayout(detailsCard, BoxLayout.Y_AXIS));
        detailsCard.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(20, 24, 20, 24)));
        detailsCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsCard.setMaximumSize(new Dimension(800, 340));

        JLabel detailsTitle = new JLabel("Assessment Details");
        detailsTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        detailsTitle.setForeground(TEXT_PRI);
        detailsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleField = new JTextField();
        UIUtils.styleTextField(titleField, 9, 12, 9, 12);
        titleField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        titleField.setAlignmentX(Component.LEFT_ALIGNMENT);

        subjectBox = new JComboBox<>(new String[]{
            "Java Programming", "Data Structures", "Algorithms",
            "Database Systems", "Operating Systems", "Computer Networks",
            "Software Engineering", "Web Development", "Mathematics", "Other"
        });
        subjectBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subjectBox.setBackground(BG_CARD2);
        subjectBox.setForeground(TEXT_PRI);
        subjectBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        subjectBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        descriptionField = new JTextArea(3, 20);
        UIUtils.styleTextField(descriptionField, 8, 10, 8, 10);
        descriptionField.setLineWrap(true);
        descriptionField.setWrapStyleWord(true);
        JScrollPane descScroll = new JScrollPane(descriptionField);
        descScroll.setBorder(null);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.getViewport().setBackground(BG_CARD2);

        detailsCard.add(detailsTitle);
        detailsCard.add(Box.createVerticalStrut(14));
        detailsCard.add(fieldLabel("ASSESSMENT TITLE"));
        detailsCard.add(Box.createVerticalStrut(6));
        detailsCard.add(titleField);
        detailsCard.add(Box.createVerticalStrut(12));
        detailsCard.add(fieldLabel("SUBJECT"));
        detailsCard.add(Box.createVerticalStrut(6));
        detailsCard.add(subjectBox);
        detailsCard.add(Box.createVerticalStrut(12));
        detailsCard.add(fieldLabel("DESCRIPTION (Optional)"));
        detailsCard.add(Box.createVerticalStrut(6));
        detailsCard.add(descScroll);

        form.add(detailsCard);
        form.add(Box.createVerticalStrut(18));

        // Config Card
        JPanel configCard = new JPanel();
        configCard.setBackground(BG_CARD);
        configCard.setLayout(new BoxLayout(configCard, BoxLayout.Y_AXIS));
        configCard.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(20, 24, 20, 24)));
        configCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        configCard.setMaximumSize(new Dimension(800, 200));

        JLabel configTitle = new JLabel("Configuration");
        configTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        configTitle.setForeground(TEXT_PRI);
        configTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        durationSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 180, 5));
        durationSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        durationSpinner.setBackground(BG_CARD2);
        durationSpinner.setForeground(TEXT_PRI);
        ((JSpinner.DefaultEditor) durationSpinner.getEditor()).getTextField().setBackground(BG_CARD2);
        ((JSpinner.DefaultEditor) durationSpinner.getEditor()).getTextField().setForeground(TEXT_PRI);

        questionSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        questionSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        questionSpinner.setBackground(BG_CARD2);
        questionSpinner.setForeground(TEXT_PRI);
        ((JSpinner.DefaultEditor) questionSpinner.getEditor()).getTextField().setBackground(BG_CARD2);
        ((JSpinner.DefaultEditor) questionSpinner.getEditor()).getTextField().setForeground(TEXT_PRI);

        JPanel grid = new JPanel(new GridLayout(1, 2, 20, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        grid.add(wrapSpinner("DURATION (MINUTES)", durationSpinner));
        grid.add(wrapSpinner("NUMBER OF QUESTIONS", questionSpinner));

        randomizeCheckBox = new JCheckBox("Randomize question order for students");
        randomizeCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        randomizeCheckBox.setForeground(TEXT_PRI);
        randomizeCheckBox.setOpaque(false);
        randomizeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        configCard.add(configTitle);
        configCard.add(Box.createVerticalStrut(12));
        configCard.add(grid);
        configCard.add(Box.createVerticalStrut(12));
        configCard.add(randomizeCheckBox);

        form.add(configCard);
        form.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setMaximumSize(new Dimension(800, 44));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cancelBtn.setBackground(BG_CARD2);
        cancelBtn.setForeground(TEXT_PRI);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.setBorder(new EmptyBorder(10, 20, 10, 20));
        cancelBtn.addActionListener(e -> {
            dispose();
            new AdminDashboardFrame(adminName).setVisible(true);
        });

        saveButton = new JButton("Save & Continue →");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setBackground(ACCENT);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.setBorder(new EmptyBorder(10, 22, 10, 22));
        saveButton.addActionListener(e -> saveAssessment());

        buttonRow.add(cancelBtn, BorderLayout.WEST);
        buttonRow.add(saveButton, BorderLayout.EAST);
        form.add(buttonRow);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        scroll.getViewport().setBackground(BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);
    }

    private JPanel wrapSpinner(String labelText, JSpinner spinner) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(fieldLabel(labelText));
        p.add(Box.createVerticalStrut(6));
        spinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        spinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(spinner);
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void saveAssessment() {
        String title   = titleField.getText().trim();
        String subject = (String) subjectBox.getSelectedItem();

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an assessment title.", "Missing Title", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int     duration   = (Integer) durationSpinner.getValue();
        int     qCount     = (Integer) questionSpinner.getValue();
        String  desc       = descriptionField.getText().trim();
        boolean randomize  = randomizeCheckBox.isSelected();

        String sql = "INSERT INTO assessments (title, subject, description, duration_minutes, question_count, randomize_questions, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, (SELECT user_id FROM users WHERE username = ? AND role = 'Admin'))";

        saveButton.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private Exception failure;

            @Override
            protected Boolean doInBackground() {
                try (Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setString(2, subject);
                    ps.setString(3, desc);
                    ps.setInt(4, duration);
                    ps.setInt(5, qCount);
                    ps.setBoolean(6, randomize);
                    ps.setString(7, adminName);
                    boolean ok = ps.executeUpdate() == 1;
                    if (ok) AuditLog.log(adminName, "CREATE_ASSESSMENT",
                            title + " (" + subject + ", " + duration + " min, " + qCount + " questions)");
                    return ok;
                } catch (Exception ex) {
                    failure = ex;
                    return false;
                }
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    if (get()) {
                        JOptionPane.showMessageDialog(
                            CreateAssessmentFrame.this,
                            "Assessment created successfully!\nYou can now add questions to it.",
                            "AssessPro",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        dispose();
                        new ManageQuestionsFrame(adminName).setVisible(true);
                    } else {
                        String msg = failure != null ? failure.getMessage() : "Unknown error";
                        JOptionPane.showMessageDialog(CreateAssessmentFrame.this, "Save failed: " + msg, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}
