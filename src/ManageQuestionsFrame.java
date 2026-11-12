import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage Questions Screen with Smart Suggestions — dark themed.
 */
public class ManageQuestionsFrame extends JFrame {

    // ── Dark Palette ──────────────────────────────────────────────────────────
    private static final Color BG_APP   = new Color(15,  17,  23);
    private static final Color BG_CARD  = new Color(26,  29,  46);
    private static final Color BG_CARD2 = new Color(37,  40,  64);
    private static final Color ACCENT   = new Color(77,  142, 247);
    private static final Color TEXT_PRI = new Color(226, 232, 240);
    private static final Color TEXT_MUT = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(45,  49,  84);

    private final String adminName;
    private int adminId = -1;

    private JComboBox<AssessmentItem> assessmentBox;
    private JLabel countLabel;
    private JTextArea questionField;
    private JTextField aField, bField, cField, dField;
    private JComboBox<String> correctBox;
    private JSpinner marksSpinner;
    private JButton saveButton;

    // Smart Suggestions Popup
    private JPopupMenu suggestionsPopup;

    public ManageQuestionsFrame(String adminName) {
        super("AssessPro – Manage Questions");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setMinimumSize(new Dimension(850, 650));
        setLocationRelativeTo(null);
        buildUI();
        loadAdminId();
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
        JLabel adminLabel = new JLabel("MANAGE QUESTIONS");
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

        // Form
        JPanel form = new JPanel();
        form.setBackground(BG_APP);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(25, 40, 35, 40));

        JLabel heading = new JLabel("Add Questions");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Select an assessment and add multiple-choice questions with smart suggestions.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(heading);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(20));

        // Question Card
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(22, 26, 22, 26)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(850, 600));

        assessmentBox = new JComboBox<>();
        assessmentBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        assessmentBox.setBackground(BG_CARD2);
        assessmentBox.setForeground(TEXT_PRI);
        assessmentBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        assessmentBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        assessmentBox.addActionListener(e -> updateCount());

        countLabel = new JLabel("Select an assessment.");
        countLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        countLabel.setForeground(ACCENT);
        countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        questionField = new JTextArea(3, 20);
        UIUtils.styleTextField(questionField, 8, 10, 8, 10);
        questionField.setLineWrap(true);
        questionField.setWrapStyleWord(true);
        setupSmartSuggestions();

        JScrollPane qScroll = new JScrollPane(questionField);
        qScroll.setBorder(null);
        qScroll.getViewport().setBackground(BG_CARD2);
        qScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        qScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        aField = createOptionField();
        bField = createOptionField();
        cField = createOptionField();
        dField = createOptionField();

        JPanel optionsGrid = new JPanel(new GridLayout(2, 2, 14, 10));
        optionsGrid.setOpaque(false);
        optionsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        optionsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        optionsGrid.add(wrapField("OPTION A", aField));
        optionsGrid.add(wrapField("OPTION B", bField));
        optionsGrid.add(wrapField("OPTION C", cField));
        optionsGrid.add(wrapField("OPTION D", dField));

        correctBox = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        correctBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        correctBox.setBackground(BG_CARD2);
        correctBox.setForeground(TEXT_PRI);
        correctBox.setPreferredSize(new Dimension(100, 36));

        marksSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        marksSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        marksSpinner.setBackground(BG_CARD2);
        marksSpinner.setForeground(TEXT_PRI);
        ((JSpinner.DefaultEditor) marksSpinner.getEditor()).getTextField().setBackground(BG_CARD2);
        ((JSpinner.DefaultEditor) marksSpinner.getEditor()).getTextField().setForeground(TEXT_PRI);
        marksSpinner.setPreferredSize(new Dimension(100, 36));

        JLabel ansLbl1 = new JLabel("Correct Answer:");
        ansLbl1.setForeground(TEXT_MUT);
        JLabel ansLbl2 = new JLabel("Marks:");
        ansLbl2.setForeground(TEXT_MUT);

        JPanel ansRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        ansRow.setOpaque(false);
        ansRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ansRow.add(ansLbl1);
        ansRow.add(correctBox);
        ansRow.add(ansLbl2);
        ansRow.add(marksSpinner);

        card.add(fieldLabel("ASSESSMENT"));
        card.add(Box.createVerticalStrut(6));
        card.add(assessmentBox);
        card.add(Box.createVerticalStrut(6));
        card.add(countLabel);
        card.add(Box.createVerticalStrut(14));
        card.add(fieldLabel("QUESTION TEXT (Type to see smart suggestions)"));
        card.add(Box.createVerticalStrut(6));
        card.add(qScroll);
        card.add(Box.createVerticalStrut(14));
        card.add(optionsGrid);
        card.add(Box.createVerticalStrut(16));
        card.add(ansRow);

        form.add(card);
        form.add(Box.createVerticalStrut(18));

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveButton = new JButton("Save Question");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setBackground(ACCENT);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.setBorder(new EmptyBorder(10, 22, 10, 22));
        saveButton.addActionListener(e -> saveQuestion());

        JButton clearButton = new JButton("Clear Fields");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.setBackground(BG_CARD2);
        clearButton.setForeground(TEXT_PRI);
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.setBorder(new EmptyBorder(10, 18, 10, 18));
        clearButton.addActionListener(e -> clearFields());

        buttonRow.add(saveButton);
        buttonRow.add(clearButton);
        form.add(buttonRow);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        scroll.getViewport().setBackground(BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);
    }

    private JTextField createOptionField() {
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

    private void setupSmartSuggestions() {
        suggestionsPopup = new JPopupMenu();
        suggestionsPopup.setBackground(BG_CARD2);
        suggestionsPopup.setBorder(new LineBorder(BORDER, 1));
        questionField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    return;
                }
                String query = questionField.getText().trim();
                AssessmentItem selected = (AssessmentItem) assessmentBox.getSelectedItem();
                if (query.length() < 3 || selected == null) {
                    suggestionsPopup.setVisible(false);
                    return;
                }
                fetchSuggestions(query, selected.subject);
            }
        });
    }

    private void fetchSuggestions(String query, String subject) {
        String sql = "SELECT DISTINCT question_text, option_a, option_b, option_c, option_d, correct_option, marks " +
                     "FROM questions WHERE subject = ? AND question_text LIKE ? LIMIT 5";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, subject);
            ps.setString(2, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                suggestionsPopup.removeAll();
                boolean hasItems = false;
                while (rs.next()) {
                    hasItems = true;
                    String qText = rs.getString("question_text");
                    String oa = rs.getString("option_a");
                    String ob = rs.getString("option_b");
                    String oc = rs.getString("option_c");
                    String od = rs.getString("option_d");
                    String corr = rs.getString("correct_option");
                    int m = rs.getInt("marks");

                    JMenuItem item = new JMenuItem("→ " + (qText.length() > 65 ? qText.substring(0, 62) + "..." : qText));
                    item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    item.setBackground(BG_CARD2);
                    item.setForeground(TEXT_PRI);
                    item.addActionListener(e -> {
                        questionField.setText(qText);
                        aField.setText(oa);
                        bField.setText(ob);
                        cField.setText(oc);
                        dField.setText(od);
                        correctBox.setSelectedItem(corr);
                        marksSpinner.setValue(m);
                        suggestionsPopup.setVisible(false);
                    });
                    suggestionsPopup.add(item);
                }
                if (hasItems) {
                    suggestionsPopup.show(questionField, 0, questionField.getHeight());
                    questionField.requestFocusInWindow();
                } else {
                    suggestionsPopup.setVisible(false);
                }
            }
        } catch (SQLException ignored) {}
    }

    private void loadAdminId() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT user_id FROM users WHERE username = ? AND role = 'Admin'")) {
            ps.setString(1, adminName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) adminId = rs.getInt(1);
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void loadAssessments() {
        assessmentBox.removeAllItems();
        String sql = "SELECT assessment_id, title, subject, question_count FROM assessments ORDER BY assessment_id DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                assessmentBox.addItem(new AssessmentItem(
                    rs.getInt("assessment_id"),
                    rs.getString("title"),
                    rs.getString("subject"),
                    rs.getInt("question_count")
                ));
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        updateCount();
    }

    private void updateCount() {
        AssessmentItem item = (AssessmentItem) assessmentBox.getSelectedItem();
        if (item == null) {
            countLabel.setText("No assessments found.");
            return;
        }
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM questions WHERE assessment_id = ?")) {
            ps.setInt(1, item.id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int added = rs.getInt(1);
                    countLabel.setText("Questions added: " + added + " / " + item.limit);
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void saveQuestion() {
        AssessmentItem item = (AssessmentItem) assessmentBox.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Please select an assessment.", "Missing Assessment", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String q    = questionField.getText().trim();
        String a    = aField.getText().trim();
        String b    = bField.getText().trim();
        String c    = cField.getText().trim();
        String d    = dField.getText().trim();
        String corr = (String) correctBox.getSelectedItem();
        int marks   = (Integer) marksSpinner.getValue();

        if (q.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || corr == null) {
            JOptionPane.showMessageDialog(this, "Please fill in the question text, all 4 options, and select the correct option.", "Incomplete Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String insertSql = "INSERT INTO questions (subject, question_text, option_a, option_b, option_c, option_d, correct_option, marks, created_by, assessment_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(insertSql)) {
            ps.setString(1, item.subject);
            ps.setString(2, q);
            ps.setString(3, a);
            ps.setString(4, b);
            ps.setString(5, c);
            ps.setString(6, d);
            ps.setString(7, corr);
            ps.setInt(8, marks);
            ps.setInt(9, adminId > 0 ? adminId : 1);
            ps.setInt(10, item.id);
            ps.executeUpdate();
            AuditLog.log(adminName, "ADD_QUESTION", item.title + ": " + AuditLog.clip(q, 80));

            ToastNotification.show(this, "Question saved successfully!", ToastNotification.Type.SUCCESS);
            clearFields();
            updateCount();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not save question:\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        questionField.setText("");
        aField.setText("");
        bField.setText("");
        cField.setText("");
        dField.setText("");
        correctBox.setSelectedIndex(0);
        marksSpinner.setValue(1);
    }

    private static class AssessmentItem {
        final int id, limit;
        final String title, subject;

        AssessmentItem(int id, String title, String subject, int limit) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.limit = limit;
        }

        @Override
        public String toString() {
            return title + " – " + subject + " (ID: " + id + ")";
        }
    }
}
