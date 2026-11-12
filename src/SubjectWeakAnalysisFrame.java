import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Subject-wise Weak Student Analysis — Faculty only.
 *
 * Landing view: clickable subject tiles showing student count, attempt count
 * and average score. Clicking a tile drills into a table of students who
 * scored below the configurable WEAK_THRESHOLD in that subject.
 */
public class SubjectWeakAnalysisFrame extends JFrame {

    // Students whose average score in a subject is below this % are "weak"
    private static final double WEAK_THRESHOLD = 60.0;

    private static final Color BG_APP   = DashboardUI.BG_APP;
    private static final Color BG_CARD  = DashboardUI.BG_CARD;
    private static final Color BG_CARD2 = DashboardUI.BG_CARD2;
    private static final Color TEXT_PRI = DashboardUI.TEXT_PRI;
    private static final Color TEXT_MUT = DashboardUI.TEXT_MUT;
    private static final Color BORDER   = DashboardUI.BORDER;
    private static final Color ACCENT   = DashboardUI.ACCENT;
    private static final Color DANGER   = DashboardUI.DANGER;
    private static final Color AMBER    = DashboardUI.AMBER;
    private static final Color EMERALD  = DashboardUI.EMERALD;

    private final String facultyName;

    // Panel that swaps between subject grid and detail view
    private final JPanel body;
    private final CardLayout cards;

    // Detail view components
    private JLabel detailTitle;
    private JLabel detailSub;
    private DefaultTableModel detailModel;
    private JTable detailTable;
    private int detailHoverRow = -1;

    public SubjectWeakAnalysisFrame(String facultyName) {
        super("AssessPro – Weak Student Analysis");
        this.facultyName = facultyName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1180, 800);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);
        setContentPane(root);

        root.add(AdminUI.subHeader("WEAK STUDENT ANALYSIS", () -> {
            dispose();
            new FacultyDashboardFrame(facultyName).setVisible(true);
        }), BorderLayout.NORTH);

        cards = new CardLayout();
        body  = new JPanel(cards);
        body.setOpaque(false);

        body.add(buildSubjectGrid(), "GRID");
        body.add(buildDetailView(), "DETAIL");
        cards.show(body, "GRID");

        root.add(body, BorderLayout.CENTER);
    }

    // ── Subject Grid (landing view) ───────────────────────────────────────────

    private JComponent buildSubjectGrid() {
        DashboardUI.Stack page = new DashboardUI.Stack();
        page.setBorder(new EmptyBorder(28, 36, 36, 36));

        JLabel heading = new JLabel("Subject-wise Weak Student Analysis");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(
                "Click a subject to see students who averaged below " + (int) WEAK_THRESHOLD
                + "% across its assessments.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(TEXT_MUT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        page.add(heading);
        page.add(Box.createVerticalStrut(6));
        page.add(sub);
        page.add(Box.createVerticalStrut(28));
        page.add(DashboardUI.sectionTitle("Available Subjects"));
        page.add(Box.createVerticalStrut(14));

        List<SubjectStat> subjects = loadSubjectStats();

        if (subjects.isEmpty()) {
            page.add(DashboardUI.muted("No assessment data found. Create some assessments first."));
        } else {
            // Wrap tiles in a flow-like grid (up to 3 per row)
            int cols = Math.min(3, subjects.size());
            JPanel grid = new JPanel(new GridLayout(0, cols, 16, 16));
            grid.setOpaque(false);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);
            grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            for (SubjectStat ss : subjects) {
                grid.add(buildSubjectTile(ss));
            }
            page.add(grid);
        }

        return DashboardUI.scroll(page);
    }

    /** One clickable subject card showing headline numbers. */
    private JComponent buildSubjectTile(SubjectStat ss) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard()
                .accent(ss.weakCount > 0 ? DANGER : EMERALD)
                .hoverable();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Subject name
        JLabel nameLbl = new JLabel(ss.subject);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLbl.setForeground(TEXT_PRI);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Assessments count badge
        JLabel assLbl = new JLabel(ss.assessmentCount + (ss.assessmentCount == 1 ? " assessment" : " assessments"));
        assLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        assLbl.setForeground(TEXT_MUT);
        assLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Stat row
        JPanel stats = new JPanel(new GridLayout(1, 2, 10, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        stats.add(miniStat("AVG SCORE", String.format("%.1f%%", ss.avgScore), ss.avgScore >= 60 ? EMERALD : AMBER));
        stats.add(miniStat("STUDENTS NEEDING SUPPORT", String.valueOf(ss.weakCount),
                ss.weakCount > 0 ? DANGER : EMERALD));

        // Hint label
        String hint = ss.weakCount == 0
                ? "All students performing well ✓"
                : ss.weakCount + (ss.weakCount == 1 ? " student needs attention" : " students need attention");
        JLabel hintLbl = new JLabel(hint);
        hintLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hintLbl.setForeground(ss.weakCount > 0 ? DANGER : EMERALD);
        hintLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(nameLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(assLbl);
        card.add(Box.createVerticalStrut(14));
        card.add(stats);
        card.add(Box.createVerticalStrut(10));
        card.add(hintLbl);

        card.onClick(() -> {
            card.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            SwingUtilities.invokeLater(() -> {
                try {
                    showDetailView(ss.subject);
                } finally {
                    card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            });
        });
        card.bindInteraction();
        return card;
    }

    private JComponent miniStat(String caption, String value, Color col) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel cap = new JLabel(caption);
        cap.setFont(new Font("Segoe UI", Font.BOLD, 9));
        cap.setForeground(TEXT_MUT);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 20));
        val.setForeground(col);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(cap);
        p.add(val);
        return p;
    }

    // ── Detail View (per-subject weak student table) ──────────────────────────

    private JComponent buildDetailView() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);

        DashboardUI.Stack page = new DashboardUI.Stack();
        page.setBorder(new EmptyBorder(28, 36, 36, 36));

        // Back row
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        backRow.setOpaque(false);
        backRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        DashboardUI.QuietButton backBtn = new DashboardUI.QuietButton("← All Subjects");
        backBtn.addActionListener(e -> cards.show(body, "GRID"));
        backRow.add(backBtn);

        detailTitle = new JLabel("Subject");
        detailTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        detailTitle.setForeground(TEXT_PRI);
        detailTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailSub = new JLabel("Students whose average score in this subject is below " + (int) WEAK_THRESHOLD + "%.");
        detailSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailSub.setForeground(TEXT_MUT);
        detailSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        page.add(backRow);
        page.add(Box.createVerticalStrut(10));
        page.add(detailTitle);
        page.add(Box.createVerticalStrut(4));
        page.add(detailSub);
        page.add(Box.createVerticalStrut(20));

        // Summary stat cards row
        page.add(DashboardUI.sectionTitle("Weak Students — Details"));
        page.add(Box.createVerticalStrut(14));

        // Table
        DashboardUI.RoundedCard tableCard = new DashboardUI.RoundedCard();
        tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS));

        detailModel = new DefaultTableModel(
                new String[]{"S.No", "Student Name", "Total Attempts", "Avg Score (%)", "Best Score (%)", "Worst Score (%)", "Status"},
                0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailTable.setForeground(TEXT_PRI);
        detailTable.setBackground(BG_CARD);
        detailTable.setRowHeight(34);
        detailTable.setShowVerticalLines(false);
        detailTable.setGridColor(BORDER);
        detailTable.setSelectionBackground(BG_CARD2);
        detailTable.setSelectionForeground(TEXT_PRI);

        detailTable.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = detailTable.rowAtPoint(e.getPoint());
                if (row != detailHoverRow) { detailHoverRow = row; detailTable.repaint(); }
            }
        });
        detailTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                detailHoverRow = -1;
                detailTable.repaint();
            }
        });

        JTableHeader th = detailTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(BG_CARD2);
        th.setForeground(TEXT_MUT);
        th.setPreferredSize(new Dimension(0, 36));
        ((DefaultTableCellRenderer) th.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Custom cell renderer with row color based on score severity
        detailTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    if (row == detailHoverRow) c.setBackground(BG_CARD2);
                    else c.setBackground(row % 2 == 0 ? BG_CARD : BG_APP);
                    c.setForeground(TEXT_PRI);

                    // Color the Avg Score column based on how weak
                    if (col == 3 && value != null) {
                        try {
                            double v = Double.parseDouble(value.toString().replace("%", ""));
                            c.setForeground(v < 40 ? DANGER : v < 50 ? new Color(249, 115, 22) : AMBER);
                        } catch (NumberFormatException ignored) {}
                    }
                    // Color the Status column
                    if (col == 6 && value != null) {
                        String s = value.toString();
                        c.setForeground(s.startsWith("Critical") ? DANGER
                                      : s.startsWith("At Risk")  ? AMBER
                                      : AMBER);
                    }
                }
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return c;
            }
        });

        // Column widths
        int[] widths = {55, 180, 130, 130, 130, 130, 120};
        for (int i = 0; i < widths.length; i++) {
            detailTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane tableScroll = new JScrollPane(detailTable);
        tableScroll.setBorder(new LineBorder(BORDER, 1, true));
        tableScroll.getViewport().setBackground(BG_CARD);
        tableScroll.setBackground(BG_CARD);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(0, 320));

        tableCard.add(tableScroll);
        page.add(tableCard);
        page.add(Box.createVerticalStrut(14));

        // Footer note
        JLabel note = DashboardUI.muted(
                "Status uses each student's average across completed attempts in this subject: "
                + "Critical < 40%, At Risk 40–49.9%, Below Average 50–59.9%. "
                + "Students at " + (int) WEAK_THRESHOLD + "% or above are not listed.");
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(note);

        root.add(DashboardUI.scroll(page), BorderLayout.CENTER);
        return root;
    }

    private void showDetailView(String subject) {
        detailTitle.setText("Weak Students — " + subject);
        detailSub.setText("Loading students...");
        cards.show(body, "DETAIL");
        SwingUtilities.invokeLater(() -> loadDetailData(subject));
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private List<SubjectStat> loadSubjectStats() {
        List<SubjectStat> list = new ArrayList<>();
        // Per-subject: how many assessments, avg score across all students,
        // and count of students whose per-subject avg is below threshold
        String sql =
            "SELECT a.subject, " +
            "  COUNT(DISTINCT a.assessment_id) AS assessment_count, " +
            "  COALESCE(AVG(ea.score * 100.0 / NULLIF(ea.total_marks, 0)), 0) AS avg_score, " +
            "  COUNT(DISTINCT CASE WHEN student_avg.avg_pct < ? THEN student_avg.student_id END) AS weak_count " +
            "FROM assessments a " +
            "LEFT JOIN exam_attempts ea ON ea.assessment_id = a.assessment_id AND UPPER(ea.status) = 'COMPLETED' AND ea.total_marks > 0 " +
            "LEFT JOIN ( " +
            "  SELECT ea2.student_id, a2.subject, " +
            "         AVG(ea2.score * 100.0 / NULLIF(ea2.total_marks, 0)) AS avg_pct " +
            "  FROM exam_attempts ea2 " +
            "  JOIN assessments a2 ON ea2.assessment_id = a2.assessment_id " +
            "  WHERE UPPER(ea2.status) = 'COMPLETED' AND ea2.total_marks > 0 " +
            "  GROUP BY ea2.student_id, a2.subject " +
            ") student_avg ON student_avg.subject = a.subject " +
            "GROUP BY a.subject " +
            "ORDER BY weak_count DESC, a.subject";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, WEAK_THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SubjectStat ss = new SubjectStat();
                    ss.subject         = rs.getString("subject");
                    ss.assessmentCount = rs.getInt("assessment_count");
                    ss.avgScore        = rs.getDouble("avg_score");
                    ss.weakCount       = rs.getInt("weak_count");
                    list.add(ss);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    private void loadDetailData(String subject) {
        detailModel.setRowCount(0);

        String sql =
            "SELECT u.full_name, " +
            "  COUNT(ea.attempt_id) AS total_attempts, " +
            "  AVG(ea.score * 100.0 / NULLIF(ea.total_marks, 0)) AS avg_pct, " +
            "  MAX(ea.score * 100.0 / NULLIF(ea.total_marks, 0)) AS best_pct, " +
            "  MIN(ea.score * 100.0 / NULLIF(ea.total_marks, 0)) AS worst_pct " +
            "FROM exam_attempts ea " +
            "JOIN users u ON u.user_id = ea.student_id " +
            "JOIN assessments a ON a.assessment_id = ea.assessment_id " +
            "WHERE a.subject = ? " +
            "  AND UPPER(ea.status) = 'COMPLETED' " +
            "  AND ea.total_marks > 0 " +
            "GROUP BY u.user_id, u.full_name " +
            "HAVING avg_pct < ? " +
            "ORDER BY avg_pct ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, subject);
            ps.setDouble(2, WEAK_THRESHOLD);
            try (ResultSet rs = ps.executeQuery()) {
                int sno = 1;
                while (rs.next()) {
                    double avg   = rs.getDouble("avg_pct");
                    double best  = rs.getDouble("best_pct");
                    double worst = rs.getDouble("worst_pct");
                    String status = avg < 40 ? "Critical — Needs Help"
                                 : avg < 50  ? "At Risk"
                                 :             "Below Average";
                    detailModel.addRow(new Object[]{
                        sno++,
                        rs.getString("full_name"),
                        rs.getInt("total_attempts"),
                        String.format("%.1f%%", avg),
                        String.format("%.1f%%", best),
                        String.format("%.1f%%", worst),
                        status
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load data: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        if (detailModel.getRowCount() == 0) {
            detailSub.setText("No students below " + (int) WEAK_THRESHOLD + "% in \"" + subject + "\". All students are performing well!");
        } else {
            detailSub.setText("Students who averaged below " + (int) WEAK_THRESHOLD + "% across assessments in \"" + subject + "\".");
        }
    }

    // ── Helper types ──────────────────────────────────────────────────────────

    private static final class SubjectStat {
        String subject;
        int assessmentCount, weakCount;
        double avgScore;
    }
}
