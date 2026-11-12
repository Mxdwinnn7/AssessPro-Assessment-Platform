import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Student Results History screen — dark themed.
 */
public class StudentResultsHistoryFrame extends JFrame {

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

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final String studentName;

    public StudentResultsHistoryFrame(String studentName) {
        super("AssessPro – My Exam Results");
        this.studentName = studentName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setMinimumSize(new Dimension(800, 560));
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);
        setContentPane(root);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_CARD);
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(14, 30, 14, 30)
        ));
        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 21));
        logo.setForeground(ACCENT);
        JLabel section = new JLabel("MY RESULTS");
        section.setFont(new Font("Segoe UI", Font.BOLD, 10));
        section.setForeground(TEXT_MUT);
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logo); branding.add(section);

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
            new StudentDashboardFrame(studentName).setVisible(true);
        });
        header.add(branding, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        return header;
    }

    private JScrollPane buildContent() {
        JPanel content = new JPanel();
        content.setBackground(BG_APP);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 50, 40, 50));

        JLabel heading = new JLabel("Exam History & Results");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("View your previous assessment scores and performance.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(heading);
        content.add(Box.createVerticalStrut(5));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(20));

        List<ResultRecord> records = loadResults();

        if (records.isEmpty()) {
            JPanel empty = new JPanel(new BorderLayout());
            empty.setBackground(BG_CARD);
            empty.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(36, 24, 36, 24)));
            JLabel emptyLbl = new JLabel("<html><center><b>No Exam Results Found</b><br>You have not completed any assessments yet.</center></html>", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            emptyLbl.setForeground(TEXT_MUT);
            empty.add(emptyLbl, BorderLayout.CENTER);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(empty);
        } else {
            for (ResultRecord rec : records) {
                JPanel card = buildResultCard(rec);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                content.add(card);
                content.add(Box.createVerticalStrut(12));
            }
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        scroll.getViewport().setBackground(BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildResultCard(ResultRecord rec) {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(18, 22, 18, 22)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel title = new JLabel(rec.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(TEXT_PRI);

        JLabel subject = new JLabel(rec.subject);
        subject.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subject.setForeground(ACCENT);

        String dateStr = rec.submittedAt != null ? "Submitted: " + rec.submittedAt.format(FMT) : "Completed";
        JLabel date = new JLabel(dateStr);
        date.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        date.setForeground(TEXT_MUT);

        JPanel infoBox = new JPanel();
        infoBox.setOpaque(false);
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.add(title); infoBox.add(Box.createVerticalStrut(4));
        infoBox.add(subject); infoBox.add(Box.createVerticalStrut(4));
        infoBox.add(date);

        double pct = rec.totalMarks > 0 ? ((double) rec.score / rec.totalMarks) * 100.0 : 0.0;
        JLabel scoreLabel = new JLabel(rec.score + " / " + rec.totalMarks);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        scoreLabel.setForeground(ACCENT);
        scoreLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel pctLabel = new JLabel(String.format("%.0f%%", pct));
        pctLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pctLabel.setForeground(pct >= 50 ? SUCCESS : DANGER);
        pctLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel scoreBox = new JPanel();
        scoreBox.setOpaque(false);
        scoreBox.setLayout(new BoxLayout(scoreBox, BoxLayout.Y_AXIS));
        scoreBox.add(scoreLabel); scoreBox.add(Box.createVerticalStrut(4)); scoreBox.add(pctLabel);

        card.add(infoBox, BorderLayout.CENTER);
        card.add(scoreBox, BorderLayout.EAST);
        return card;
    }

    private List<ResultRecord> loadResults() {
        List<ResultRecord> list = new ArrayList<>();
        String sql = "SELECT ea.attempt_id, a.title, a.subject, ea.score, ea.total_marks, ea.submitted_at " +
                     "FROM exam_attempts ea JOIN assessments a ON ea.assessment_id = a.assessment_id " +
                     "JOIN users u ON ea.student_id = u.user_id " +
                     "WHERE u.username = ? AND UPPER(ea.status) = 'COMPLETED' ORDER BY ea.submitted_at DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("submitted_at");
                    list.add(new ResultRecord(rs.getInt("attempt_id"), rs.getString("title"),
                            rs.getString("subject"), rs.getInt("score"), rs.getInt("total_marks"),
                            ts != null ? ts.toLocalDateTime() : null));
                }
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
        return list;
    }

    private static class ResultRecord {
        final int attemptId, score, totalMarks;
        final String title, subject;
        final LocalDateTime submittedAt;
        ResultRecord(int attemptId, String title, String subject, int score, int totalMarks, LocalDateTime submittedAt) {
            this.attemptId = attemptId; this.title = title; this.subject = subject;
            this.score = score; this.totalMarks = totalMarks; this.submittedAt = submittedAt;
        }
    }
}
