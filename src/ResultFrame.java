import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Result screen shown after exam submission — dark themed.
 */
public class ResultFrame extends JFrame {

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

    private final int    score, totalMarks, correctCount, totalQuestions;
    private final String studentName, assessmentTitle;

    public ResultFrame(int score, int totalMarks, int correctCount, int totalQuestions,
                       String studentName, String assessmentTitle) {
        super("AssessPro – Exam Result");
        this.score = score; this.totalMarks = totalMarks;
        this.correctCount = correctCount; this.totalQuestions = totalQuestions;
        this.studentName = studentName; this.assessmentTitle = assessmentTitle;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);
        setMinimumSize(new Dimension(800, 580));
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
            new EmptyBorder(14, 36, 14, 36)
        ));
        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT);
        header.add(logo, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Result card
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(36, 52, 36, 52)
        ));

        JLabel titleLabel = label("EXAM RESULT", 28, Font.BOLD, TEXT_PRI);
        JLabel examLabel  = label(assessmentTitle != null && !assessmentTitle.isBlank() ? assessmentTitle : "Assessment", 17, Font.PLAIN, TEXT_MUT);

        double percentage = totalMarks > 0 ? ((double) score / totalMarks) * 100.0 : 0.0;
        JLabel scoreLabel = label(score + " / " + totalMarks, 48, Font.BOLD, ACCENT);
        JLabel pctLabel   = label(String.format("%.0f%%", percentage), 22, Font.BOLD,
                                  percentage >= 50 ? SUCCESS : DANGER);

        int wrongCount = Math.max(0, totalQuestions - correctCount);
        JLabel correctLabel = new JLabel("<html><center>✔ Correct<br>" + correctCount + " / " + totalQuestions + "</center></html>");
        correctLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        correctLabel.setForeground(SUCCESS);
        correctLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel wrongLabel = new JLabel("<html><center>✘ Wrong<br>" + wrongCount + "</center></html>");
        wrongLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        wrongLabel.setForeground(DANGER);
        wrongLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel statsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsRow.add(correctLabel);
        statsRow.add(wrongLabel);

        JLabel statusLabel = label("Assessment Completed", 14, Font.PLAIN, TEXT_MUT);

        JButton dashBtn = new JButton("BACK TO DASHBOARD");
        dashBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dashBtn.setBackground(ACCENT);
        dashBtn.setForeground(Color.WHITE);
        dashBtn.setFocusPainted(false);
        dashBtn.setBorderPainted(false);
        dashBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dashBtn.setBorder(new EmptyBorder(12, 28, 12, 28));
        dashBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        dashBtn.addActionListener(e -> {
            dispose();
            new StudentDashboardFrame(studentName).setVisible(true);
        });

        for (Component c : new Component[]{titleLabel, examLabel, scoreLabel, pctLabel, statsRow, statusLabel, dashBtn}) {
            if (c instanceof JLabel l) l.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(c);
            card.add(Box.createVerticalStrut(c == pctLabel ? 16 : 12));
        }

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_APP);
        center.setBorder(new EmptyBorder(40, 60, 40, 60));
        card.setMaximumSize(new Dimension(600, 520));
        center.add(card);
        root.add(center, BorderLayout.CENTER);
    }

    private JLabel label(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }
}
