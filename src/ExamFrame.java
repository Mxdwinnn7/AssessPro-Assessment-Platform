import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Exam screen — dark themed. Full-screen timed examination.
 */
public class ExamFrame extends JFrame {

    // ── Dark Palette ──────────────────────────────────────────────────────────
    private static final Color BG_APP   = new Color(15,  17,  23);
    private static final Color BG_CARD  = new Color(26,  29,  46);
    private static final Color BG_CARD2 = new Color(37,  40,  64);
    private static final Color ACCENT   = new Color(77,  142, 247);
    private static final Color TEXT_PRI = new Color(226, 232, 240);
    private static final Color TEXT_MUT = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(45,  49,  84);
    private static final Color DANGER   = new Color(239, 68,  68);

    private final int    assessmentId;
    private final String studentName;
    private int          studentId = -1;

    private String  assessmentTitle = "Assessment";
    private int     durationMinutes = 30;
    private boolean randomize       = false;
    private int     targetCount     = 0;

    private final List<QuestionItem> questions = new ArrayList<>();
    private int   currentIndex = 0;
    private int   timeLeft;
    private Timer timer;
    private boolean submitted = false;
    private long    startedAtMillis;

    // UI refs
    private JLabel questionNumberLabel;
    private JLabel questionTextLabel;
    private JLabel timerLabel;
    private JRadioButton optA, optB, optC, optD;
    private ButtonGroup optGroup;
    private JButton prevBtn, nextBtn, submitBtn;

    public ExamFrame(String studentName, int assessmentId, String title, int durationMinutes) {
        super("AssessPro – Examination");
        this.assessmentId    = assessmentId;
        this.studentName     = studentName;
        this.assessmentTitle = title;
        this.durationMinutes = durationMinutes;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                    ExamFrame.this,
                    "Are you sure you want to exit? Your progress will not be saved.",
                    "Exit Exam",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (result == JOptionPane.YES_OPTION) {
                    if (timer != null) timer.stop();
                    dispose();
                    new StudentDashboardFrame(studentName).setVisible(true);
                }
            }
        });
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) { super.setVisible(false); return; }

        boolean loaded = loadAssessmentAndQuestions();
        if (!loaded || questions.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                "No questions are available for this assessment. Returning to dashboard.",
                "Exam Unavailable", JOptionPane.ERROR_MESSAGE);
            new StudentDashboardFrame(studentName).setVisible(true);
            return;
        }

        startedAtMillis = System.currentTimeMillis();
        buildUI();
        super.setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        showQuestion();
        startTimer();
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
            new EmptyBorder(14, 36, 14, 36)
        ));

        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT);

        JLabel titleLabel = new JLabel("  |  " + assessmentTitle);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        titleLabel.setForeground(TEXT_MUT);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(logo);
        left.add(titleLabel);

        timerLabel = new JLabel(formatTime(timeLeft));
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        timerLabel.setForeground(ACCENT);
        timerLabel.setOpaque(true);
        timerLabel.setBackground(BG_CARD2);
        timerLabel.setBorder(new EmptyBorder(8, 16, 8, 16));

        header.add(left, BorderLayout.WEST);
        header.add(timerLabel, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Question card
        JPanel questionCard = new JPanel();
        questionCard.setBackground(BG_CARD);
        questionCard.setLayout(new BoxLayout(questionCard, BoxLayout.Y_AXIS));
        questionCard.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(32, 40, 32, 40)
        ));

        questionNumberLabel = new JLabel();
        questionNumberLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        questionNumberLabel.setForeground(TEXT_MUT);
        questionNumberLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        questionTextLabel = new JLabel();
        questionTextLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        questionTextLabel.setForeground(TEXT_PRI);
        questionTextLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        optGroup = new ButtonGroup();
        optA = createOption(); optB = createOption();
        optC = createOption(); optD = createOption();

        questionCard.add(questionNumberLabel);
        questionCard.add(Box.createVerticalStrut(16));
        questionCard.add(questionTextLabel);
        questionCard.add(Box.createVerticalStrut(22));
        questionCard.add(optA); questionCard.add(Box.createVerticalStrut(12));
        questionCard.add(optB); questionCard.add(Box.createVerticalStrut(12));
        questionCard.add(optC); questionCard.add(Box.createVerticalStrut(12));
        questionCard.add(optD);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_APP);
        center.setBorder(new EmptyBorder(30, 50, 10, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1; gbc.weighty = 1;
        questionCard.setMaximumSize(new Dimension(900, Integer.MAX_VALUE));
        center.add(questionCard, gbc);
        root.add(center, BorderLayout.CENTER);

        // Navigation footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 16));
        footer.setBackground(BG_APP);

        prevBtn   = navBtn("PREVIOUS", BG_CARD2, TEXT_PRI);
        nextBtn   = navBtn("NEXT",     ACCENT,   Color.WHITE);
        submitBtn = navBtn("SUBMIT EXAM", DANGER, Color.WHITE);

        prevBtn.addActionListener(e -> showPrevious());
        nextBtn.addActionListener(e -> showNext());
        submitBtn.addActionListener(e -> confirmSubmit());

        footer.add(prevBtn);
        footer.add(nextBtn);
        footer.add(submitBtn);
        root.add(footer, BorderLayout.SOUTH);
    }

    private JRadioButton createOption() {
        JRadioButton rb = new JRadioButton();
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        rb.setForeground(TEXT_PRI);
        rb.setBackground(BG_CARD);
        rb.setOpaque(false);
        rb.setAlignmentX(Component.LEFT_ALIGNMENT);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        optGroup.add(rb);
        return rb;
    }

    private JButton navBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 24, 10, 24));
        return btn;
    }

    private void showQuestion() {
        if (questions.isEmpty()) return;
        QuestionItem q = questions.get(currentIndex);
        questionNumberLabel.setText("QUESTION " + (currentIndex + 1) + " OF " + questions.size() +
                "  (" + q.marks + (q.marks == 1 ? " mark)" : " marks)"));
        questionTextLabel.setText("<html><div style='width:750px'>" + q.text + "</div></html>");
        optA.setText("A.  " + q.optA);
        optB.setText("B.  " + q.optB);
        optC.setText("C.  " + q.optC);
        optD.setText("D.  " + q.optD);
        optGroup.clearSelection();
        if      (q.selected == 'A') optA.setSelected(true);
        else if (q.selected == 'B') optB.setSelected(true);
        else if (q.selected == 'C') optC.setSelected(true);
        else if (q.selected == 'D') optD.setSelected(true);
        prevBtn.setEnabled(currentIndex > 0);
        nextBtn.setEnabled(currentIndex < questions.size() - 1);
    }

    private void saveCurrentAnswer() {
        if (questions.isEmpty()) return;
        QuestionItem q = questions.get(currentIndex);
        if      (optA.isSelected()) q.selected = 'A';
        else if (optB.isSelected()) q.selected = 'B';
        else if (optC.isSelected()) q.selected = 'C';
        else if (optD.isSelected()) q.selected = 'D';
    }

    private void showNext()     { saveCurrentAnswer(); if (currentIndex < questions.size() - 1) { currentIndex++; showQuestion(); } }
    private void showPrevious() { saveCurrentAnswer(); if (currentIndex > 0) { currentIndex--; showQuestion(); } }

    private void confirmSubmit() {
        saveCurrentAnswer();
        int unanswered = (int) questions.stream().filter(q -> q.selected == ' ').count();
        String msg = unanswered > 0
            ? "You have " + unanswered + " unanswered question(s). Submit anyway?"
            : "Are you sure you want to submit your exam?";
        int r = JOptionPane.showConfirmDialog(this, msg, "Submit Exam", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) submitExam();
    }

    private void submitExam() {
        if (submitted) return;
        submitted = true;
        if (timer != null) timer.stop();
        submitBtn.setEnabled(false); prevBtn.setEnabled(false); nextBtn.setEnabled(false);
        saveCurrentAnswer();

        int totalScore = 0, totalMarks = 0, correctCount = 0;
        for (QuestionItem q : questions) {
            totalMarks += q.marks;
            if (q.selected != ' ' && Character.toUpperCase(q.selected) == Character.toUpperCase(q.correct)) {
                totalScore += q.marks;
                correctCount++;
            }
        }

        saveExamResults(totalScore, totalMarks);

        final int fs = totalScore, fm = totalMarks, fc = correctCount;
        SwingUtilities.invokeLater(() -> {
            dispose();
            new ResultFrame(fs, fm, fc, questions.size(), studentName, assessmentTitle).setVisible(true);
        });
    }

    private void saveExamResults(int totalScore, int totalMarks) {
        if (studentId <= 0) studentId = lookupStudentId();
        if (studentId <= 0) return;

        String insertAttempt = "INSERT INTO exam_attempts (assessment_id, student_id, started_at, submitted_at, score, total_marks, status) VALUES (?, ?, ?, NOW(), ?, ?, 'COMPLETED')";
        String insertAnswer  = "INSERT INTO student_answers (attempt_id, question_id, selected_option, is_correct, marks_awarded) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            int attemptId = -1;
            try (PreparedStatement ps = con.prepareStatement(insertAttempt, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, assessmentId); ps.setInt(2, studentId);
                ps.setTimestamp(3, new Timestamp(startedAtMillis));
                ps.setInt(4, totalScore); ps.setInt(5, totalMarks);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) attemptId = keys.getInt(1);
                }
            }
            if (attemptId > 0) {
                try (PreparedStatement ps = con.prepareStatement(insertAnswer)) {
                    for (QuestionItem q : questions) {
                        ps.setInt(1, attemptId); ps.setInt(2, q.id);
                        if (q.selected == ' ') ps.setNull(3, Types.CHAR);
                        else ps.setString(3, String.valueOf(q.selected));
                        boolean correct = q.selected != ' ' && Character.toUpperCase(q.selected) == Character.toUpperCase(q.correct);
                        ps.setBoolean(4, correct);
                        ps.setInt(5, correct ? q.marks : 0);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }
            con.commit();
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void startTimer() {
        timer = new Timer(1000, e -> {
            if (timeLeft <= 0) {
                ((Timer) e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "Time is up! Your exam will be submitted now.", "Time Up", JOptionPane.WARNING_MESSAGE);
                submitExam();
                return;
            }
            timeLeft--;
            timerLabel.setText(formatTime(timeLeft));
            if (timeLeft == 300) {
                ToastNotification.show(this, "5 minutes remaining! Review your answers.", ToastNotification.Type.WARNING);
            } else if (timeLeft == 60) {
                ToastNotification.show(this, "Final 60 seconds remaining!", ToastNotification.Type.ERROR);
            }
            if (timeLeft <= 60) {
                timerLabel.setForeground(DANGER);
                timerLabel.setBackground(new Color(60, 20, 20));
            }
        });
        timer.start();
    }

    private String formatTime(int seconds) {
        int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? String.format("%02d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    private boolean loadAssessmentAndQuestions() {
        try (Connection con = DatabaseConnection.getConnection()) {
            String sql1 = "SELECT title, duration_minutes, randomize_questions, question_count FROM assessments WHERE assessment_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sql1)) {
                ps.setInt(1, assessmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    assessmentTitle = rs.getString("title");
                    durationMinutes = Math.max(1, rs.getInt("duration_minutes"));
                    randomize       = rs.getBoolean("randomize_questions");
                    targetCount     = rs.getInt("question_count");
                    timeLeft        = durationMinutes * 60;
                }
            }
            String sql2 = "SELECT user_id FROM users WHERE username = ?";
            try (PreparedStatement ps = con.prepareStatement(sql2)) {
                ps.setString(1, studentName);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) studentId = rs.getInt(1); }
            }
            String sql3 = "SELECT question_id, question_text, option_a, option_b, option_c, option_d, correct_option, marks FROM questions WHERE assessment_id = ? ORDER BY question_id";
            try (PreparedStatement ps = con.prepareStatement(sql3)) {
                ps.setInt(1, assessmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String corr = rs.getString("correct_option");
                        char correctChar = (corr != null && !corr.isEmpty()) ? corr.charAt(0) : 'A';
                        questions.add(new QuestionItem(rs.getInt("question_id"),
                                rs.getString("question_text"), rs.getString("option_a"),
                                rs.getString("option_b"),      rs.getString("option_c"),
                                rs.getString("option_d"),      correctChar, rs.getInt("marks")));
                    }
                }
            }
            if (questions.isEmpty()) return false;
            if (randomize) Collections.shuffle(questions);
            if (targetCount > 0 && questions.size() > targetCount)
                questions.subList(targetCount, questions.size()).clear();
            return true;
        } catch (SQLException ex) { ex.printStackTrace(); return false; }
    }

    private int lookupStudentId() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return -1;
    }

    private static class QuestionItem {
        final int id, marks;
        final String text, optA, optB, optC, optD;
        final char correct;
        char selected = ' ';

        QuestionItem(int id, String text, String optA, String optB, String optC, String optD, char correct, int marks) {
            this.id = id; this.text = text; this.optA = optA; this.optB = optB;
            this.optC = optC; this.optD = optD; this.correct = correct; this.marks = marks;
        }
    }
}
