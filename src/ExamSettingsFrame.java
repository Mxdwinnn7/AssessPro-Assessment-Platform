import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exam Settings Screen — dark themed.
 */
public class ExamSettingsFrame extends JFrame {

    // ── Dark Palette ──────────────────────────────────────────────────────────
    private static final Color BG_APP   = new Color(15,  17,  23);
    private static final Color BG_CARD  = new Color(26,  29,  46);
    private static final Color BG_CARD2 = new Color(37,  40,  64);
    private static final Color ACCENT   = new Color(77,  142, 247);
    private static final Color TEXT_PRI = new Color(226, 232, 240);
    private static final Color TEXT_MUT = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(45,  49,  84);
    private static final Color DANGER   = new Color(239, 68,  68);

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final String adminName;

    private JComboBox<AssessmentItem> assessmentBox;
    private JSpinner   durationSpinner;
    private JCheckBox  randomizeCheckBox;
    private JTextField startDateField, startTimeField;
    private JTextField endDateField,   endTimeField;
    private JButton    saveButton, deleteButton;

    public ExamSettingsFrame(String adminName) {
        super("AssessPro – Exam Settings");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 720);
        setMinimumSize(new Dimension(850, 620));
        setLocationRelativeTo(null);
        buildUI();
        loadAssessments();
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
        JLabel panel = new JLabel("EXAM SETTINGS");
        panel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        panel.setForeground(TEXT_MUT);
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logo);
        branding.add(panel);

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

        JLabel heading = new JLabel("Exam Settings");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Configure assessment duration, question randomization, and availability window.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(heading);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(20));

        // Settings Card
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(22, 26, 22, 26)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(850, 500));

        assessmentBox = new JComboBox<>();
        assessmentBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        assessmentBox.setBackground(BG_CARD2);
        assessmentBox.setForeground(TEXT_PRI);
        assessmentBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        assessmentBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        assessmentBox.addActionListener(e -> loadSelectedDetails());

        durationSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 180, 5));
        durationSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        durationSpinner.setBackground(BG_CARD2);
        durationSpinner.setForeground(TEXT_PRI);
        ((JSpinner.DefaultEditor) durationSpinner.getEditor()).getTextField().setBackground(BG_CARD2);
        ((JSpinner.DefaultEditor) durationSpinner.getEditor()).getTextField().setForeground(TEXT_PRI);
        durationSpinner.setPreferredSize(new Dimension(160, 38));
        durationSpinner.setMaximumSize(new Dimension(160, 38));
        durationSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);

        randomizeCheckBox = new JCheckBox("Randomize question order for students");
        randomizeCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        randomizeCheckBox.setForeground(TEXT_PRI);
        randomizeCheckBox.setOpaque(false);
        randomizeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        startDateField = createDateField("YYYY-MM-DD");
        startTimeField = createDateField("HH:mm");
        endDateField   = createDateField("YYYY-MM-DD");
        endTimeField   = createDateField("HH:mm");

        JPanel dateGrid = new JPanel(new GridLayout(2, 2, 20, 10));
        dateGrid.setOpaque(false);
        dateGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        dateGrid.add(wrapField("AVAILABLE FROM (DATE: YYYY-MM-DD)", startDateField));
        dateGrid.add(wrapField("START TIME (HH:MM, e.g. 09:00)", startTimeField));
        dateGrid.add(wrapField("AVAILABLE UNTIL (DATE: YYYY-MM-DD)", endDateField));
        dateGrid.add(wrapField("END TIME (HH:MM, e.g. 18:00)", endTimeField));

        card.add(fieldLabel("SELECT ASSESSMENT"));
        card.add(Box.createVerticalStrut(6));
        card.add(assessmentBox);
        card.add(Box.createVerticalStrut(14));
        JPanel durationRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        durationRow.setOpaque(false);
        durationRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        durationRow.add(durationSpinner);

        card.add(fieldLabel("DURATION (MINUTES)"));
        card.add(Box.createVerticalStrut(6));
        card.add(durationRow);
        card.add(Box.createVerticalStrut(12));
        card.add(randomizeCheckBox);
        card.add(Box.createVerticalStrut(16));
        card.add(dateGrid);

        form.add(card);
        form.add(Box.createVerticalStrut(20));

        // Buttons
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setMaximumSize(new Dimension(850, 44));

        deleteButton = new JButton("Delete Assessment");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        deleteButton.setBackground(DANGER);
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        deleteButton.addActionListener(e -> deleteAssessment());

        saveButton = new JButton("Save Changes");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setBackground(ACCENT);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.setBorder(new EmptyBorder(10, 22, 10, 22));
        saveButton.addActionListener(e -> saveSettings());

        buttonRow.add(deleteButton, BorderLayout.WEST);
        buttonRow.add(saveButton, BorderLayout.EAST);
        form.add(buttonRow);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        scroll.getViewport().setBackground(BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);
    }

    private JTextField createDateField(String prompt) {
        JTextField f = new JTextField();
        UIUtils.styleTextField(f, 8, 12, 8, 12);
        return f;
    }

    private JPanel wrapField(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(TEXT_MUT);
        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(field);
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void loadAssessments() {
        assessmentBox.removeAllItems();
        String sql = "SELECT assessment_id, title, subject, duration_minutes, randomize_questions, available_from, available_until " +
                     "FROM assessments ORDER BY assessment_id DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                assessmentBox.addItem(new AssessmentItem(
                    rs.getInt("assessment_id"),
                    rs.getString("title"),
                    rs.getString("subject"),
                    rs.getInt("duration_minutes"),
                    rs.getBoolean("randomize_questions"),
                    rs.getTimestamp("available_from"),
                    rs.getTimestamp("available_until")
                ));
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        loadSelectedDetails();
    }

    private void loadSelectedDetails() {
        AssessmentItem item = (AssessmentItem) assessmentBox.getSelectedItem();
        if (item == null) return;
        durationSpinner.setValue(item.duration);
        randomizeCheckBox.setSelected(item.randomize);

        if (item.availableFrom != null) {
            LocalDateTime ldt = item.availableFrom.toLocalDateTime();
            startDateField.setText(ldt.toLocalDate().toString());
            startTimeField.setText(ldt.toLocalTime().format(TIME_FORMAT));
        } else {
            startDateField.setText("");
            startTimeField.setText("");
        }

        if (item.availableUntil != null) {
            LocalDateTime ldt = item.availableUntil.toLocalDateTime();
            endDateField.setText(ldt.toLocalDate().toString());
            endTimeField.setText(ldt.toLocalTime().format(TIME_FORMAT));
        } else {
            endDateField.setText("");
            endTimeField.setText("");
        }
    }

    private void saveSettings() {
        AssessmentItem item = (AssessmentItem) assessmentBox.getSelectedItem();
        if (item == null) return;

        int     duration  = (Integer) durationSpinner.getValue();
        boolean randomize = randomizeCheckBox.isSelected();

        Timestamp fromTs  = parseTimestamp(startDateField.getText().trim(), startTimeField.getText().trim());
        Timestamp untilTs = parseTimestamp(endDateField.getText().trim(), endTimeField.getText().trim());

        String sql = "UPDATE assessments SET duration_minutes = ?, randomize_questions = ?, available_from = ?, available_until = ? WHERE assessment_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, duration);
            ps.setBoolean(2, randomize);
            if (fromTs  != null) ps.setTimestamp(3, fromTs);  else ps.setNull(3, Types.TIMESTAMP);
            if (untilTs != null) ps.setTimestamp(4, untilTs); else ps.setNull(4, Types.TIMESTAMP);
            ps.setInt(5, item.id);
            ps.executeUpdate();
            AuditLog.log(adminName, "UPDATE_SETTINGS", item.title + " — " + duration + " min, randomize "
                    + (randomize ? "on" : "off")
                    + ", available " + (fromTs != null ? fromTs.toString().substring(0, 16) : "any time")
                    + " to " + (untilTs != null ? untilTs.toString().substring(0, 16) : "no end"));

            ToastNotification.show(this, "Settings updated successfully!", ToastNotification.Type.SUCCESS);
            loadAssessments();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving settings: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteAssessment() {
        AssessmentItem item = (AssessmentItem) assessmentBox.getSelectedItem();
        if (item == null) return;

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this assessment and all its questions and attempts?\n" + item.title,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement p1 = con.prepareStatement("DELETE FROM student_answers WHERE attempt_id IN (SELECT attempt_id FROM exam_attempts WHERE assessment_id = ?)");
                 PreparedStatement p2 = con.prepareStatement("DELETE FROM exam_attempts WHERE assessment_id = ?");
                 PreparedStatement p3 = con.prepareStatement("DELETE FROM questions WHERE assessment_id = ?");
                 PreparedStatement p4 = con.prepareStatement("DELETE FROM assessments WHERE assessment_id = ?")) {
                p1.setInt(1, item.id); p1.executeUpdate();
                p2.setInt(1, item.id); p2.executeUpdate();
                p3.setInt(1, item.id); p3.executeUpdate();
                p4.setInt(1, item.id); p4.executeUpdate();
                con.commit();
            }
            AuditLog.log(adminName, "DELETE_ASSESSMENT", item.title + " (with its questions and attempts)");
            JOptionPane.showMessageDialog(this, "Assessment deleted successfully.", "AssessPro", JOptionPane.INFORMATION_MESSAGE);
            loadAssessments();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not delete assessment: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Timestamp parseTimestamp(String dateStr, String timeStr) {
        if (dateStr.isEmpty()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalTime time = timeStr.isEmpty() ? LocalTime.of(0, 0) : LocalTime.parse(timeStr, TIME_FORMAT);
            return Timestamp.valueOf(LocalDateTime.of(date, time));
        } catch (Exception ex) {
            return null;
        }
    }

    private static class AssessmentItem {
        final int id, duration;
        final String title, subject;
        final boolean randomize;
        final Timestamp availableFrom, availableUntil;

        AssessmentItem(int id, String title, String subject, int duration, boolean randomize, Timestamp availableFrom, Timestamp availableUntil) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.duration = duration;
            this.randomize = randomize;
            this.availableFrom = availableFrom;
            this.availableUntil = availableUntil;
        }

        @Override
        public String toString() {
            return title + " – " + subject + " (ID: " + id + ")";
        }
    }
}
