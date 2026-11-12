import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Admin Dashboard — bento actions, live statistics, and system status.
 */
public class AdminDashboardFrame extends JFrame {

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm:ss");

    private final String adminName;
    private Timer clockTimer;
    private DashboardUI.Stack analyticsHost;

    public AdminDashboardFrame(String adminName) {
        super("AssessPro – Admin Dashboard");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(980, 680));
        setLocationRelativeTo(null);
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(DashboardUI.BG_APP);
        setContentPane(root);
        root.add(DashboardUI.header("ADMIN PANEL", () -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }), BorderLayout.NORTH);

        DashboardUI.Stack page = new DashboardUI.Stack();
        page.setBorder(new EmptyBorder(24, 32, 40, 32));

        JLabel welcome = new JLabel("Welcome, " + adminName + "!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(DashboardUI.TEXT_PRI);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        page.add(welcome);
        page.add(Box.createVerticalStrut(4));
        page.add(DashboardUI.muted("Manage assessments, questions and exam settings."));
        page.add(Box.createVerticalStrut(18));
        page.add(systemBar());
        page.add(Box.createVerticalStrut(18));

        int active = dbCount("SELECT COUNT(*) FROM assessments " +
                "WHERE (available_from IS NULL OR available_from <= NOW()) " +
                "AND (available_until IS NULL OR available_until >= NOW())");
        int questions = dbCount("SELECT COUNT(*) FROM questions");
        int students = dbCount("SELECT COUNT(*) FROM users WHERE role = 'Student'");
        int completed = dbCount("SELECT COUNT(DISTINCT assessment_id) FROM exam_attempts WHERE UPPER(status) = 'COMPLETED'");
        int assessments = dbCount("SELECT COUNT(*) FROM assessments");
        AuditLog.ensureTable();
        int loggedActions = dbCount("SELECT COUNT(*) FROM audit_log");

        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));
        stats.add(statCard("ACTIVE ASSESSMENTS", active, "Open right now", DashboardUI.ACCENT));
        stats.add(statCard("TOTAL QUESTIONS", questions, "Questions available", DashboardUI.EMERALD));
        stats.add(statCard("REGISTERED STUDENTS", students, "Student accounts", DashboardUI.ACCENT));
        stats.add(statCard("COMPLETED ASSESSMENTS", completed, "With at least one result", DashboardUI.EMERALD));
        page.add(stats);
        page.add(Box.createVerticalStrut(26));
        page.add(DashboardUI.sectionTitle("Management"));
        page.add(Box.createVerticalStrut(14));

        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setPreferredSize(new Dimension(920, 262));
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 262));
        grid.add(actionCard(DashboardUI.Glyph.CLIPBOARD, DashboardUI.ACCENT, "Create Assessment",
                "Create an assessment and configure its details.",
                countText(assessments, "total assessment", "total assessments"),
                () -> open(new CreateAssessmentFrame(adminName))));
        grid.add(actionCard(DashboardUI.Glyph.DATABASE, DashboardUI.EMERALD, "Manage Questions",
                "Add, edit and remove multiple-choice questions.",
                countText(questions, "question", "questions"),
                () -> open(new ManageQuestionsFrame(adminName))));
        grid.add(actionCard(DashboardUI.Glyph.SLIDERS, DashboardUI.AMBER, "Exam Settings",
                "Configure duration, availability, and question randomization.",
                countText(active, "active assessment", "active assessments"),
                () -> open(new ExamSettingsFrame(adminName))));
        grid.add(actionCard(DashboardUI.Glyph.USERS, DashboardUI.ACCENT, "User Management",
                "Add, view and remove Student and Faculty users.",
                countText(students, "registered student", "registered students"),
                () -> open(new ManageUsersFrame(adminName))));

        JComponent auditCard = actionCard(DashboardUI.Glyph.LOG, DashboardUI.AMBER, "Audit Log",
                "Review who created, changed or deleted what, and when.",
                countText(loggedActions, "logged action", "logged actions"),
                () -> open(new AuditLogFrame(adminName)));
        page.add(grid);
        page.add(Box.createVerticalStrut(14));
        auditCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 124));
        page.add(auditCard);

        // Analytics + activity feed
        page.add(Box.createVerticalStrut(30));
        JPanel analyticsHead = new JPanel(new BorderLayout());
        analyticsHead.setOpaque(false);
        analyticsHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        analyticsHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        analyticsHead.add(DashboardUI.sectionTitle("Platform Analytics"), BorderLayout.WEST);
        DashboardUI.QuietButton refresh = new DashboardUI.QuietButton("Refresh");
        refresh.addActionListener(e -> refreshAnalytics());
        analyticsHead.add(refresh, BorderLayout.EAST);
        page.add(analyticsHead);
        page.add(Box.createVerticalStrut(14));
        analyticsHost = new DashboardUI.Stack();
        page.add(analyticsHost);
        refreshAnalytics();

        root.add(DashboardUI.scroll(page), BorderLayout.CENTER);
    }

    private JComponent systemBar() {
        boolean connected = false;
        int students = -1;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'Student'");
             ResultSet rs = ps.executeQuery()) {
            connected = true;
            if (rs.next()) students = rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        DashboardUI.RoundedCard bar = new DashboardUI.RoundedCard();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        row.setOpaque(false);

        row.add(cluster("Database", DashboardUI.badge(connected ? "Connected" : "Unavailable",
                connected ? DashboardUI.EMERALD : DashboardUI.DANGER)));
        row.add(cluster("Students", new JLabel(students < 0 ? "–" : String.valueOf(students))));
        JLabel clock = new JLabel(LocalDateTime.now().format(CLOCK_FMT));
        clock.setFont(DashboardUI.UI_BOLD);
        clock.setForeground(DashboardUI.TEXT_PRI);
        row.add(cluster("System Time", clock));
        bar.add(row, BorderLayout.CENTER);

        clockTimer = new Timer(1000, e -> clock.setText(LocalDateTime.now().format(CLOCK_FMT)));
        clockTimer.start();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                clockTimer.stop();
            }
        });
        return bar;
    }

    private JPanel cluster(String caption, JComponent value) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(caption.toUpperCase());
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(DashboardUI.TEXT_MUT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (value instanceof JLabel && value.getFont().getSize() < 13) {
            value.setFont(DashboardUI.UI_BOLD);
            value.setForeground(DashboardUI.TEXT_PRI);
        }
        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
        panel.add(value);
        return panel;
    }

    private JComponent statCard(String heading, int value, String description, Color accent) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().accent(accent).hoverable();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setForeground(DashboardUI.TEXT_MUT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel n = new JLabel(value < 0 ? "–" : String.valueOf(value));
        n.setFont(new Font("Segoe UI", Font.BOLD, 28));
        n.setForeground(accent);
        n.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel d = DashboardUI.muted(description);
        card.add(h);
        card.add(Box.createVerticalStrut(4));
        card.add(n);
        card.add(Box.createVerticalStrut(2));
        card.add(d);
        card.bindInteraction();
        return card;
    }

    private JComponent actionCard(DashboardUI.Glyph glyph, Color color, String title, String description,
                                  String stat, Runnable action) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().onClick(action);
        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        JPanel iconHolder = new JPanel(new BorderLayout());   // keeps the icon box small, top-aligned
        iconHolder.setOpaque(false);
        iconHolder.add(DashboardUI.iconWell(glyph, color), BorderLayout.NORTH);
        body.add(iconHolder, BorderLayout.WEST);

        DashboardUI.Stack text = new DashboardUI.Stack();
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        t.setForeground(DashboardUI.TEXT_PRI);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel d = new JLabel("<html><div style='width:190px;line-height:1.35'>" + description + "</div></html>");
        d.setFont(DashboardUI.UI);
        d.setForeground(DashboardUI.TEXT_MUT);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel s = new JLabel(stat);
        s.setFont(DashboardUI.UI_BOLD);
        s.setForeground(color);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(t);
        text.add(Box.createVerticalStrut(6));
        text.add(d);
        text.add(Box.createVerticalStrut(10));
        text.add(s);
        body.add(text, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);
        card.bindInteraction();
        return card;
    }


    // ── Platform analytics (charts) and recent activity ────────────────────────

    private static final Color[] SCORE_BUCKET_COLORS = {
        DashboardUI.DANGER, new Color(249, 115, 22), DashboardUI.AMBER, DashboardUI.ACCENT, DashboardUI.EMERALD
    };
    private static final String[] SCORE_BUCKET_LABELS = {"0–19", "20–39", "40–59", "60–79", "80+"};
    private static final Color[] PIE_COLORS = {
        DashboardUI.ACCENT, DashboardUI.EMERALD, DashboardUI.AMBER, new Color(168, 85, 247),
        new Color(236, 72, 153), new Color(20, 184, 166), new Color(249, 115, 22), DashboardUI.SILVER
    };

    private void refreshAnalytics() {
        analyticsHost.removeAll();
        try (Connection con = DatabaseConnection.getConnection()) {
            JPanel charts = new JPanel(new GridLayout(1, 3, 16, 0));
            charts.setOpaque(false);
            charts.setAlignmentX(Component.LEFT_ALIGNMENT);
            charts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));
            charts.add(chartCard("Exam Attempts · Last 14 Days", attemptsChart(con)));
            charts.add(chartCard("Score Distribution (% of marks)", scoreChart(con)));
            charts.add(chartCard("Assessments by Subject", subjectChart(con)));
            analyticsHost.add(charts);
            analyticsHost.add(Box.createVerticalStrut(28));
            analyticsHost.add(DashboardUI.sectionTitle("Recent Activity"));
            analyticsHost.add(Box.createVerticalStrut(14));
            analyticsHost.add(activityCard(con));
        } catch (SQLException ex) {
            ex.printStackTrace();
            analyticsHost.add(DashboardUI.muted("Analytics are unavailable right now: " + ex.getMessage()));
        }
        analyticsHost.revalidate();
        analyticsHost.repaint();
    }

    private JComponent chartCard(String title, JComponent body) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard();
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(DashboardUI.TEXT_PRI);
        t.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(t, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JComponent chartPlaceholder(String message) {
        JPanel ph = new JPanel(new GridBagLayout());
        ph.setOpaque(false);
        ph.setPreferredSize(new Dimension(300, 230));
        JLabel l = new JLabel(message);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(DashboardUI.TEXT_MUT);
        ph.add(l);
        return ph;
    }

    private ChartPanel chartPanel(JFreeChart chart) {
        chart.setBackgroundPaint(DashboardUI.BG_CARD);
        chart.setAntiAlias(true);
        ChartPanel cp = new ChartPanel(chart);
        cp.setBackground(DashboardUI.BG_CARD);
        cp.setPreferredSize(new Dimension(300, 230));
        cp.setMinimumDrawWidth(120);
        cp.setMinimumDrawHeight(120);
        cp.setDomainZoomable(false);
        cp.setRangeZoomable(false);
        cp.setPopupMenu(null);
        return cp;
    }

    private void styleCategoryPlot(CategoryPlot plot, boolean rotateLabels) {
        plot.setBackgroundPaint(DashboardUI.BG_CARD);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(DashboardUI.BORDER);
        CategoryAxis domain = plot.getDomainAxis();
        domain.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        domain.setTickLabelPaint(DashboardUI.TEXT_MUT);
        domain.setAxisLinePaint(DashboardUI.BORDER);
        domain.setTickMarksVisible(false);
        if (rotateLabels) domain.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        range.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        range.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        range.setTickLabelPaint(DashboardUI.TEXT_MUT);
        range.setAxisLineVisible(false);
        range.setTickMarksVisible(false);
        range.setLowerBound(0);
    }

    /** Line chart: exams submitted per day over the last 14 days (days with none show as 0). */
    private JComponent attemptsChart(Connection con) throws SQLException {
        LocalDate today = LocalDate.now();
        java.util.Map<LocalDate, Integer> perDay = new java.util.HashMap<>();
        String sql = "SELECT DATE(submitted_at) AS d, COUNT(*) AS n FROM exam_attempts " +
                     "WHERE submitted_at >= DATE_SUB(CURDATE(), INTERVAL 13 DAY) GROUP BY DATE(submitted_at)";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.sql.Date d = rs.getDate("d");
                if (d != null) perDay.put(d.toLocalDate(), rs.getInt("n"));
            }
        }
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM");
        int total = 0;
        for (int i = 13; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            int n = perDay.getOrDefault(day, 0);
            total += n;
            data.addValue(n, "Attempts", day.format(fmt));
        }
        if (total == 0) return chartPlaceholder("No exam attempts in the last 14 days.");

        JFreeChart chart = ChartFactory.createLineChart(null, null, null, data,
                PlotOrientation.VERTICAL, false, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        styleCategoryPlot(plot, true);
        LineAndShapeRenderer r = new LineAndShapeRenderer(true, true);
        r.setSeriesPaint(0, DashboardUI.ACCENT);
        r.setSeriesStroke(0, new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        r.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        plot.setRenderer(r);
        return chartPanel(chart);
    }

    /** Bar chart: how many completed attempts fall in each score band (as % of total marks). */
    private JComponent scoreChart(Connection con) throws SQLException {
        int[] buckets = new int[5];
        String sql = "SELECT score * 100.0 / total_marks AS pct FROM exam_attempts " +
                     "WHERE UPPER(status) = 'COMPLETED' AND total_marks > 0";
        int total = 0;
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double pct = Math.max(0, Math.min(100, rs.getDouble("pct")));
                buckets[Math.min(4, (int) (pct / 20))]++;
                total++;
            }
        }
        if (total == 0) return chartPlaceholder("No completed exams yet.");

        DefaultCategoryDataset data = new DefaultCategoryDataset();
        for (int i = 0; i < 5; i++) data.addValue(buckets[i], "Attempts", SCORE_BUCKET_LABELS[i]);

        JFreeChart chart = ChartFactory.createBarChart(null, null, null, data,
                PlotOrientation.VERTICAL, false, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        styleCategoryPlot(plot, false);
        BarRenderer r = new BarRenderer() {
            @Override
            public Paint getItemPaint(int row, int column) {
                return SCORE_BUCKET_COLORS[Math.min(column, SCORE_BUCKET_COLORS.length - 1)];
            }
        };
        r.setBarPainter(new StandardBarPainter());
        r.setShadowVisible(false);
        r.setMaximumBarWidth(0.14);
        plot.setRenderer(r);
        return chartPanel(chart);
    }

    /** Pie chart: number of assessments per subject. */
    @SuppressWarnings("unchecked")
    private JComponent subjectChart(Connection con) throws SQLException {
        DefaultPieDataset<String> data = new DefaultPieDataset<>();
        String sql = "SELECT COALESCE(NULLIF(subject, ''), 'Other') AS subj, COUNT(*) AS n " +
                     "FROM assessments GROUP BY COALESCE(NULLIF(subject, ''), 'Other') ORDER BY n DESC";
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) data.setValue(rs.getString("subj") + " (" + rs.getInt("n") + ")", rs.getInt("n"));
        }
        if (data.getItemCount() == 0) return chartPlaceholder("No assessments created yet.");

        JFreeChart chart = ChartFactory.createPieChart(null, data, true, true, false);
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(DashboardUI.BG_CARD);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelGenerator(null);
        plot.setSectionOutlinesVisible(false);
        plot.setInteriorGap(0.04);
        int i = 0;
        for (Comparable<?> key : data.getKeys()) {
            plot.setSectionPaint((String) key, PIE_COLORS[i++ % PIE_COLORS.length]);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 11));
            chart.getLegend().setItemPaint(DashboardUI.TEXT_PRI);
            chart.getLegend().setBackgroundPaint(DashboardUI.BG_CARD);
        }
        return chartPanel(chart);
    }

    // ── Recent activity feed (exam submissions + admin actions) ────────────────

    private static final class Activity {
        final Timestamp when;
        final Color color;
        final String text;
        Activity(Timestamp when, Color color, String text) { this.when = when; this.color = color; this.text = text; }
    }

    private JComponent activityCard(Connection con) throws SQLException {
        List<Activity> items = new ArrayList<>();

        String examSql = "SELECT e.submitted_at, u.full_name, a.title, e.score, e.total_marks " +
                         "FROM exam_attempts e " +
                         "JOIN users u ON u.user_id = e.student_id " +
                         "JOIN assessments a ON a.assessment_id = e.assessment_id " +
                         "WHERE e.submitted_at IS NOT NULL ORDER BY e.submitted_at DESC LIMIT 8";
        try (PreparedStatement ps = con.prepareStatement(examSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int score = rs.getInt("score"), total = rs.getInt("total_marks");
                String result = total > 0 ? Math.round(100.0 * score / total) + "%" : score + " marks";
                items.add(new Activity(rs.getTimestamp("submitted_at"), DashboardUI.EMERALD,
                        rs.getString("full_name") + " completed " + rs.getString("title") + " — " + result));
            }
        }

        String auditSql = "SELECT created_at, actor, action, details FROM audit_log ORDER BY created_at DESC LIMIT 8";
        try (PreparedStatement ps = con.prepareStatement(auditSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String details = rs.getString("details");
                items.add(new Activity(rs.getTimestamp("created_at"), DashboardUI.ACCENT,
                        rs.getString("actor") + " · " + AuditLogFrame.labelFor(rs.getString("action"))
                                + (details == null || details.isEmpty() ? "" : ": " + details)));
            }
        }

        items.sort(Comparator.comparing((Activity a) -> a.when).reversed());

        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        if (items.isEmpty()) {
            card.add(DashboardUI.muted("No activity yet. Exam submissions and admin actions will appear here."));
        } else {
            int shown = Math.min(8, items.size());
            for (int i = 0; i < shown; i++) card.add(activityRow(items.get(i), i == shown - 1));
        }
        return card;
    }

    private JComponent activityRow(Activity a, boolean last) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(last ? new EmptyBorder(9, 0, 3, 0)
                : new CompoundBorder(new MatteBorder(0, 0, 1, 0, DashboardUI.BORDER), new EmptyBorder(9, 0, 9, 0)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JComponent dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(a.color);
                g2.fillOval(1, (getHeight() - 9) / 2, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 16));

        JLabel text = new JLabel(a.text);
        text.setFont(DashboardUI.UI);
        text.setForeground(DashboardUI.TEXT_PRI);
        text.setToolTipText(a.text);

        JLabel time = new JLabel(relativeTime(a.when));
        time.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        time.setForeground(DashboardUI.TEXT_MUT);

        row.add(dot, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(time, BorderLayout.EAST);
        return row;
    }

    private static String relativeTime(Timestamp ts) {
        if (ts == null) return "";
        LocalDateTime t = ts.toLocalDateTime();
        long mins = Duration.between(t, LocalDateTime.now()).toMinutes();
        if (mins < 1) return "just now";
        if (mins < 60) return mins + " min ago";
        if (mins < 24 * 60) return (mins / 60) + " h ago";
        if (mins < 48 * 60) return "yesterday";
        return t.format(DateTimeFormatter.ofPattern("MMM d"));
    }

    private void open(JFrame frame) {
        dispose();
        frame.setVisible(true);
    }

    private String countText(int value, String singular, String plural) {
        if (value < 0) return "Unavailable";
        return value + " " + (value == 1 ? singular : plural);
    }

    private int dbCount(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }
}
