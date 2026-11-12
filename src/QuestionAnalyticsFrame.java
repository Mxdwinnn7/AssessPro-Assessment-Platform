import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionAnalyticsFrame extends JFrame {
    private static final int REVIEW_MIN_ANSWERS = 3;
    private static final double REVIEW_BELOW = 30.0;

    private final String facultyName;
    private DefaultTableModel model;
    private JComboBox<AssessmentOption> assessmentBox;
    private JLabel analysedValue;
    private JLabel rateValue;
    private JLabel reviewValue;
    private JLabel hardestLabel;

    public QuestionAnalyticsFrame(String facultyName) {
        super("AssessPro – Question Analytics");
        this.facultyName = facultyName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1180, 780);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        buildUI();
        loadAssessments();
        loadAnalytics();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(DashboardUI.BG_APP);
        setContentPane(root);
        root.add(AdminUI.subHeader("QUESTION ANALYTICS", () -> {
            dispose();
            SwingUtilities.invokeLater(() -> new FacultyDashboardFrame(facultyName).setVisible(true));
        }), BorderLayout.NORTH);

        DashboardUI.Stack content = new DashboardUI.Stack();
        content.setBorder(new EmptyBorder(24, 32, 32, 32));

        JLabel heading = new JLabel("Question Analytics");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(DashboardUI.TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(4));
        content.add(DashboardUI.muted("See which questions students get right or wrong, and which ones may need a second look."));
        content.add(Box.createVerticalStrut(18));

        analysedValue = new JLabel("–");
        rateValue = new JLabel("–");
        reviewValue = new JLabel("–");

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        statsRow.add(statCard("QUESTIONS ANSWERED", analysedValue, "Questions with at least one answer", DashboardUI.ACCENT));
        statsRow.add(statCard("OVERALL CORRECT RATE", rateValue, "Correct answers across all attempts", DashboardUI.EMERALD));
        statsRow.add(statCard("NEED REVIEW", reviewValue, "Under 30% correct (min. 3 answers)", DashboardUI.DANGER));
        content.add(statsRow);
        content.add(Box.createVerticalStrut(20));

        DashboardUI.RoundedCard tableCard = new DashboardUI.RoundedCard();
        tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS));
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.setBorder(BorderFactory.createEmptyBorder(14, 14, 16, 14));

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterBar.setOpaque(false);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel assessmentLabel = new JLabel("Assessment:");
        assessmentLabel.setForeground(DashboardUI.TEXT_MUT);
        assessmentLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        assessmentBox = new JComboBox<>();
        AdminUI.flatCombo(assessmentBox);
        assessmentBox.setPreferredSize(new Dimension(300, 34));

        DashboardUI.PrimaryButton applyBtn = new DashboardUI.PrimaryButton("Apply");
        applyBtn.addActionListener(e -> loadAnalytics());

        DashboardUI.QuietButton exportBtn = new DashboardUI.QuietButton("Export to CSV");
        exportBtn.addActionListener(e -> exportCsv());

        filterBar.add(assessmentLabel);
        filterBar.add(assessmentBox);
        filterBar.add(applyBtn);
        filterBar.add(exportBtn);
        tableCard.add(filterBar);
        tableCard.add(Box.createVerticalStrut(12));

        hardestLabel = new JLabel("Hardest question: —");
        hardestLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        hardestLabel.setForeground(DashboardUI.AMBER);
        hardestLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(hardestLabel);
        tableCard.add(Box.createVerticalStrut(8));

        String[] columns = {"Question", "Answered", "Correct", "Correct %", "Most common wrong answer"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = AdminUI.darkTable(model);
        table.setRowHeight(34);
        table.setSelectionBackground(DashboardUI.BG_CARD2);
        table.setSelectionForeground(DashboardUI.TEXT_PRI);
        table.setShowVerticalLines(false);
        table.setGridColor(DashboardUI.BORDER);
        table.setFillsViewportHeight(true);
        table.setDefaultRenderer(Object.class, new AdminUI.StripedRenderer());

        JScrollPane scroll = AdminUI.tableScroll(table);
        scroll.setPreferredSize(new Dimension(0, 420));
        tableCard.add(scroll);

        content.add(tableCard);
        root.add(DashboardUI.scroll(content), BorderLayout.CENTER);
    }

    private JComponent statCard(String heading, JLabel valueLabel, String description, Color accent) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().accent(accent).hoverable();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setForeground(DashboardUI.TEXT_MUT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accent);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel d = new JLabel(description);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        d.setForeground(DashboardUI.TEXT_MUT);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(h);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(d);
        card.bindInteraction();
        return card;
    }

    private void loadAssessments() {
        AssessmentOption all = new AssessmentOption(0, "All assessments");
        DefaultComboBoxModel<AssessmentOption> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement(all);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT assessment_id, title FROM assessments ORDER BY title");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                comboModel.addElement(new AssessmentOption(rs.getInt("assessment_id"), rs.getString("title")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to load assessments:\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        assessmentBox.setModel(comboModel);
        assessmentBox.setSelectedItem(all);
    }

    private void loadAnalytics() {
        model.setRowCount(0);
        analysedValue.setText("–");
        rateValue.setText("–");
        reviewValue.setText("–");
        hardestLabel.setText("Hardest question: —");

        AssessmentOption selected = (AssessmentOption) assessmentBox.getSelectedItem();
        int assessmentId = selected == null ? 0 : selected.assessmentId;

        try (Connection con = DatabaseConnection.getConnection()) {
            int answered = 0;
            int reviewed = 0;
            double correctRate = 0.0;
            int totalAnswers = 0;
            int correctAnswers = 0;

            String answeredSql = "SELECT COUNT(DISTINCT q.question_id) FROM questions q " +
                    "WHERE (? = 0 OR q.assessment_id = ?)";
            try (PreparedStatement ps = con.prepareStatement(answeredSql)) {
                ps.setInt(1, assessmentId);
                ps.setInt(2, assessmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) answered = rs.getInt(1);
                }
            }

            String accuracySql = "SELECT COUNT(*) AS total_answers, SUM(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) AS correct_answers " +
                    "FROM student_answers sa " +
                    "JOIN exam_attempts ea ON ea.attempt_id = sa.attempt_id " +
                    "WHERE (? = 0 OR ea.assessment_id = ?)";
            try (PreparedStatement ps = con.prepareStatement(accuracySql)) {
                ps.setInt(1, assessmentId);
                ps.setInt(2, assessmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalAnswers = rs.getInt("total_answers");
                        correctAnswers = rs.getInt("correct_answers");
                    }
                }
            }

            if (totalAnswers > 0) {
                correctRate = (correctAnswers * 100.0) / totalAnswers;
            }
            analysedValue.setText(String.valueOf(answered));
            rateValue.setText(totalAnswers == 0 ? "N/A" : String.format("%.1f%%", correctRate));

            String reviewSql = "SELECT COUNT(*) FROM (" +
                    "SELECT sa.question_id FROM student_answers sa " +
                    "JOIN exam_attempts ea ON ea.attempt_id = sa.attempt_id " +
                    "WHERE (? = 0 OR ea.assessment_id = ?) " +
                    "GROUP BY sa.question_id HAVING COUNT(sa.attempt_id) >= ? AND (SUM(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(sa.attempt_id)) < ?" +
                    ") q";
            try (PreparedStatement ps = con.prepareStatement(reviewSql)) {
                ps.setInt(1, assessmentId);
                ps.setInt(2, assessmentId);
                ps.setInt(3, REVIEW_MIN_ANSWERS);
                ps.setDouble(4, REVIEW_BELOW);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) reviewed = rs.getInt(1);
                }
            }
            reviewValue.setText(String.valueOf(reviewed));

            List<Row> rows = new ArrayList<>();
            String questionSql = "SELECT q.question_id, q.question_text, q.assessment_id, " +
                    "COUNT(sa.attempt_id) AS answered, " +
                    "SUM(CASE WHEN sa.is_correct = 1 THEN 1 ELSE 0 END) AS correct_count " +
                    "FROM questions q " +
                    "LEFT JOIN student_answers sa ON sa.question_id = q.question_id " +
                    "LEFT JOIN exam_attempts ea ON ea.attempt_id = sa.attempt_id " +
                    "WHERE (? = 0 OR q.assessment_id = ?) " +
                    "GROUP BY q.question_id, q.question_text, q.assessment_id " +
                    "ORDER BY q.question_id";
            try (PreparedStatement ps = con.prepareStatement(questionSql)) {
                ps.setInt(1, assessmentId);
                ps.setInt(2, assessmentId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int questionId = rs.getInt("question_id");
                        String questionText = rs.getString("question_text");
                        int answeredCount = rs.getInt("answered");
                        int correctCount = rs.getInt("correct_count");
                        int wrongCount = Math.max(0, answeredCount - correctCount);
                        String wrongMost = mostChosenWrong(con, questionId);
                        double pct = answeredCount == 0 ? 0.0 : (correctCount * 100.0 / answeredCount);
                        rows.add(new Row(questionId, questionText, answeredCount, correctCount, pct, wrongMost));
                    }
                }
            }

            String hardestQuestion = "—";
            double hardestPct = 100.0;
            for (Row row : rows) {
                if (row.answered >= REVIEW_MIN_ANSWERS && row.correctPercent < hardestPct) {
                    hardestPct = row.correctPercent;
                    hardestQuestion = row.questionText;
                }
            }
            hardestLabel.setText("Hardest question: " + (hardestQuestion.equals("—") ? "No review candidates" : hardestQuestion));

            for (Row row : rows) {
                model.addRow(new Object[]{
                        row.questionText,
                        row.answered,
                        row.correct,
                        row.answered == 0 ? "0%" : String.format("%.1f%%", row.correctPercent),
                        row.wrongMost == null || row.wrongMost.isEmpty() ? "—" : row.wrongMost
                });
            }

            if (model.getRowCount() == 0) {
                model.addRow(new Object[]{"No question data available", "0", "0", "0%", "—"});
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to analyse questions:\n" + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String mostChosenWrong(Connection con, int questionId) throws SQLException {
        String sql = "SELECT selected_option, COUNT(*) AS choice_count FROM student_answers " +
                "WHERE question_id = ? AND is_correct = 0 AND selected_option IS NOT NULL " +
                "GROUP BY selected_option ORDER BY choice_count DESC, selected_option LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("selected_option");
                }
            }
        }
        return "—";
    }

    private void exportCsv() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No question analytics to export.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Question Analytics");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File("question_analytics.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write('\uFEFF');
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) sb.append(',');
                sb.append(csvEscape(model.getColumnName(c)));
            }
            w.write(sb.append("\r\n").toString());
            for (int r = 0; r < model.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) sb.append(',');
                    Object value = model.getValueAt(r, c);
                    sb.append(csvEscape(value == null ? "" : value.toString()));
                }
                w.write(sb.append("\r\n").toString());
            }
            JOptionPane.showMessageDialog(this, "Exported: " + file.getName(), "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static final class AssessmentOption {
        private final int assessmentId;
        private final String title;

        private AssessmentOption(int assessmentId, String title) {
            this.assessmentId = assessmentId;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private static final class Row {
        private final int questionId;
        private final String questionText;
        private final int answered;
        private final int correct;
        private final double correctPercent;
        private final String wrongMost;

        private Row(int questionId, String questionText, int answered, int correct, double correctPercent, String wrongMost) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.answered = answered;
            this.correct = correct;
            this.correctPercent = correctPercent;
            this.wrongMost = wrongMost;
        }
    }
}
