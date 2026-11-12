import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.ui.RectangleEdge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Faculty Dashboard — dark themed, JFreeChart charts with dark styling.
 */
public class FacultyDashboardFrame extends JFrame {

    private static final Color BG_APP   = DashboardUI.BG_APP;
    private static final Color BG_CARD  = DashboardUI.BG_CARD;
    private static final Color BG_CARD2 = DashboardUI.BG_CARD2;
    private static final Color ACCENT   = DashboardUI.ACCENT;
    private static final Color TEXT_PRI = DashboardUI.TEXT_PRI;
    private static final Color TEXT_MUT = DashboardUI.TEXT_MUT;
    private static final Color BORDER   = DashboardUI.BORDER;

    private final String facultyName;
    private int hoverRow = -1;
    private DashboardUI.Stack topList;
    private DashboardUI.Stack helpList;

    // Stat labels
    private JLabel totalStudentsValue;
    private JLabel examAttemptsValue;
    private JLabel averageScoreValue;
    private JLabel assessmentsValue;

    // Chart containers
    private JPanel scoreChartPanel;
    private JPanel completionChartPanel;

    // Student performance table & search
    private DefaultTableModel tableModel;
    private JTable studentTable;
    private JTextField searchField;

    public FacultyDashboardFrame(String facultyName) {
        super("AssessPro – Faculty Dashboard");
        this.facultyName = facultyName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 800);
        setMinimumSize(new Dimension(960, 680));
        setLocationRelativeTo(null);
        buildUI();
        loadDashboardData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);
        setContentPane(root);
        root.add(DashboardUI.header("FACULTY PORTAL", () -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }), BorderLayout.NORTH);

        DashboardUI.Stack content = new DashboardUI.Stack();
        content.setBorder(new EmptyBorder(24, 32, 36, 32));

        JLabel welcome = new JLabel("Welcome, " + facultyName + "!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(TEXT_PRI);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Monitor student performance, examination records and assessment analytics.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(welcome);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(22));

        // 4 Stat Cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 124));

        totalStudentsValue = new JLabel("–");
        examAttemptsValue  = new JLabel("–");
        averageScoreValue  = new JLabel("–");
        assessmentsValue   = new JLabel("–");

        statsRow.add(buildStatCard("TOTAL STUDENTS", totalStudentsValue, "Registered students"));
        statsRow.add(buildStatCard("EXAM ATTEMPTS",  examAttemptsValue,  "Total exam attempts"));
        statsRow.add(buildStatCard("AVERAGE SCORE",  averageScoreValue,  "Across completed exams"));
        statsRow.add(buildStatCard("ASSESSMENTS",    assessmentsValue,   "Assessments conducted"));

        content.add(statsRow);
        content.add(Box.createVerticalStrut(26));

        // Analytics Heading
        content.add(DashboardUI.sectionTitle("Performance Analytics"));
        content.add(Box.createVerticalStrut(14));

        // Charts Row
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);
        chartsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        scoreChartPanel      = buildChartCard("Average Score by Assessment");
        completionChartPanel = buildChartCard("Exam Completion Overview");

        chartsRow.add(scoreChartPanel);
        chartsRow.add(completionChartPanel);

        content.add(chartsRow);
        content.add(Box.createVerticalStrut(26));
        content.add(buildLeaderboard());
        content.add(Box.createVerticalStrut(26));

        // Faculty analysis tools
        content.add(DashboardUI.sectionTitle("Analysis Tools"));
        content.add(Box.createVerticalStrut(14));

        int answeredQ = 0;
        int weakStudents = 0;
        try (Connection conQ = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = conQ.prepareStatement("SELECT COUNT(DISTINCT question_id) FROM student_answers");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) answeredQ = rs.getInt(1);
            }
            // Rough count: distinct students below 60% average in any subject
            String weakSql = "SELECT COUNT(DISTINCT ea.student_id) FROM exam_attempts ea " +
                "WHERE UPPER(ea.status) = 'COMPLETED' AND ea.total_marks > 0 " +
                "GROUP BY ea.student_id HAVING AVG(ea.score * 100.0 / ea.total_marks) < 60";
            try (PreparedStatement ps = conQ.prepareStatement(
                    "SELECT COUNT(*) FROM (" + weakSql + ") t");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) weakStudents = rs.getInt(1);
            }
        } catch (SQLException ignored) {}

        final int fAnsweredQ = answeredQ;
        final int fWeakStudents = weakStudents;

        JPanel toolsGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        toolsGrid.setOpaque(false);
        toolsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 134));

        // Question Analytics card
        DashboardUI.RoundedCard qaCard = new DashboardUI.RoundedCard().accent(ACCENT).hoverable();
        qaCard.setLayout(new BoxLayout(qaCard, BoxLayout.Y_AXIS));
        qaCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel qaTitle = new JLabel("Question Analytics");
        qaTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        qaTitle.setForeground(TEXT_PRI);
        qaTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel qaDesc = new JLabel("See which questions students find hard or need review.");
        qaDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        qaDesc.setForeground(TEXT_MUT);
        qaDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel qaCount = new JLabel(fAnsweredQ + (fAnsweredQ == 1 ? " question analysed" : " questions analysed"));
        qaCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        qaCount.setForeground(ACCENT);
        qaCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        qaCard.add(qaTitle);
        qaCard.add(Box.createVerticalStrut(4));
        qaCard.add(qaDesc);
        qaCard.add(Box.createVerticalGlue());
        qaCard.add(qaCount);
        qaCard.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
                new QuestionAnalyticsFrame(facultyName).setVisible(true);
            }
        });
        qaCard.bindInteraction();

        // Weak Student Analysis card
        DashboardUI.RoundedCard weakCard = new DashboardUI.RoundedCard().accent(DashboardUI.DANGER).hoverable();
        weakCard.setLayout(new BoxLayout(weakCard, BoxLayout.Y_AXIS));
        weakCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel weakTitle = new JLabel("Weak Student Analysis");
        weakTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        weakTitle.setForeground(TEXT_PRI);
        weakTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel weakDesc = new JLabel("Subject-wise breakdown of students who need extra support.");
        weakDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        weakDesc.setForeground(TEXT_MUT);
        weakDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel weakCount = new JLabel(fWeakStudents + (fWeakStudents == 1 ? " student below 60%" : " students below 60%"));
        weakCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        weakCount.setForeground(fWeakStudents > 0 ? DashboardUI.DANGER : DashboardUI.EMERALD);
        weakCount.setAlignmentX(Component.LEFT_ALIGNMENT);
        weakCard.add(weakTitle);
        weakCard.add(Box.createVerticalStrut(4));
        weakCard.add(weakDesc);
        weakCard.add(Box.createVerticalGlue());
        weakCard.add(weakCount);
        weakCard.onClick(() -> {
            dispose();
            new SubjectWeakAnalysisFrame(facultyName).setVisible(true);
        });
        weakCard.bindInteraction();

        toolsGrid.add(qaCard);
        toolsGrid.add(weakCard);
        content.add(toolsGrid);
        content.add(Box.createVerticalStrut(26));

        content.add(DashboardUI.sectionTitle("Student Performance"));
        content.add(Box.createVerticalStrut(14));

        DashboardUI.RoundedCard tableCard = new DashboardUI.RoundedCard();
        tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = new JTextField(22);
        UIUtils.styleTextField(searchField, 7, 10, 7, 10);
        searchField.addActionListener(e -> searchStudents());

        DashboardUI.PrimaryButton searchBtn = new DashboardUI.PrimaryButton("Search");
        searchBtn.addActionListener(e -> searchStudents());

        DashboardUI.QuietButton refreshBtn = new DashboardUI.QuietButton("Refresh");
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadDashboardData();
        });

        DashboardUI.QuietButton exportBtn = new DashboardUI.QuietButton("Export to CSV");
        exportBtn.addActionListener(e -> exportCsv());

        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(refreshBtn);
        searchBar.add(exportBtn);
        tableCard.add(searchBar);
        tableCard.add(Box.createVerticalStrut(14));

        String[] columns = {"S.No", "Student Name", "Attempts", "Average Score", "Latest Result"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        studentTable = new JTable(tableModel);
        studentTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        studentTable.setForeground(TEXT_PRI);
        studentTable.setBackground(BG_CARD);
        studentTable.setRowHeight(32);
        studentTable.setShowVerticalLines(false);
        studentTable.setGridColor(BORDER);
        studentTable.setSelectionBackground(BG_CARD2);
        studentTable.setSelectionForeground(TEXT_PRI);
        studentTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = studentTable.rowAtPoint(e.getPoint());
                if (row != hoverRow) {
                    hoverRow = row;
                    studentTable.repaint();
                }
            }
        });
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                studentTable.repaint();
            }
        });

        JTableHeader th = studentTable.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(BG_CARD2);
        th.setForeground(TEXT_MUT);
        th.setPreferredSize(new Dimension(0, 35));
        ((DefaultTableCellRenderer) th.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    if (row == hoverRow) c.setBackground(BG_CARD2);
                    else c.setBackground(row % 2 == 0 ? BG_CARD : BG_APP);
                    c.setForeground(TEXT_PRI);
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        for (int i = 0; i < studentTable.getColumnCount(); i++) {
            studentTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(studentTable);
        tableScroll.setBorder(new LineBorder(BORDER, 1, true));
        tableScroll.getViewport().setBackground(BG_CARD);
        tableScroll.setBackground(BG_CARD);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(0, 250));
        tableCard.add(tableScroll);

        content.add(tableCard);

        root.add(DashboardUI.scroll(content), BorderLayout.CENTER);
    }

    private JComponent buildLeaderboard() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        DashboardUI.RoundedCard topCard = new DashboardUI.RoundedCard().accent(DashboardUI.GOLD).hoverable();
        topCard.setLayout(new BoxLayout(topCard, BoxLayout.Y_AXIS));
        topCard.add(DashboardUI.sectionTitle("Top Performers"));
        topCard.add(Box.createVerticalStrut(4));
        topCard.add(DashboardUI.muted("Highest overall average"));
        topCard.add(Box.createVerticalStrut(12));
        topList = new DashboardUI.Stack();
        topCard.add(topList);
        topCard.bindInteraction();

        DashboardUI.RoundedCard helpCard = new DashboardUI.RoundedCard().accent(DashboardUI.DANGER).hoverable();
        helpCard.setLayout(new BoxLayout(helpCard, BoxLayout.Y_AXIS));
        helpCard.add(DashboardUI.sectionTitle("Students Needing Assistance"));
        helpCard.add(Box.createVerticalStrut(4));
        helpCard.add(DashboardUI.muted("Completed attempts below 50%"));
        helpCard.add(Box.createVerticalStrut(12));
        helpList = new DashboardUI.Stack();
        helpCard.add(helpList);
        helpCard.bindInteraction();

        row.add(topCard);
        row.add(helpCard);
        return row;
    }

    private JPanel buildStatCard(String heading, JLabel valueLabel, String description) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().accent(ACCENT).hoverable();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setForeground(TEXT_MUT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(ACCENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel d = new JLabel(description);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        d.setForeground(TEXT_MUT);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(h);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(d);
        card.bindInteraction();
        return card;
    }

    private JPanel buildChartCard(String title) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().hoverable();
        card.bindInteraction();

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(TEXT_PRI);
        t.setBorder(new EmptyBorder(0, 0, 8, 0));
        card.add(t, BorderLayout.NORTH);

        JPanel placeholder = new JPanel(new GridBagLayout());
        placeholder.setBackground(BG_CARD);
        JLabel phLabel = new JLabel("Loading chart data...");
        phLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        phLabel.setForeground(TEXT_MUT);
        placeholder.add(phLabel);
        card.add(placeholder, BorderLayout.CENTER);
        return card;
    }

    private void styleActionButton(JButton btn, Color bg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
    }

    // ── Load Dashboard Data ────────────────────────────────────────────────────

    private void loadDashboardData() {
        try (Connection con = DatabaseConnection.getConnection()) {
            loadStatistics(con);
            loadScoreChart(con);
            loadCompletionChart(con);
            loadLeaderboards(con);
            loadStudentTable(con, "");
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load Faculty Dashboard data:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStatistics(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'Student'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) totalStudentsValue.setText(String.valueOf(rs.getInt(1)));
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM exam_attempts");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) examAttemptsValue.setText(String.valueOf(rs.getInt(1)));
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT AVG(score) FROM exam_attempts WHERE UPPER(status) = 'COMPLETED'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double avg = rs.getDouble(1);
                averageScoreValue.setText(rs.wasNull() ? "N/A" : String.format("%.2f", avg));
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM assessments");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) assessmentsValue.setText(String.valueOf(rs.getInt(1)));
        }
    }

    // ── Score Chart (JFreeChart Bar — dark themed) ─────────────────────────────

    private void loadScoreChart(Connection con) throws SQLException {
        String sql = "SELECT a.title, AVG(e.score) AS average_score, SUM(q.marks) AS total_marks " +
                     "FROM assessments a " +
                     "JOIN exam_attempts e ON a.assessment_id = e.assessment_id " +
                     "LEFT JOIN questions q ON a.assessment_id = q.assessment_id " +
                     "WHERE UPPER(e.status) = 'COMPLETED' " +
                     "GROUP BY a.assessment_id, a.title ORDER BY a.assessment_id";

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String title = rs.getString("title");
                double avg = rs.getDouble("average_score");
                double tot = rs.getDouble("total_marks");
                if (tot > 0) {
                    dataset.addValue(Math.min(100.0, (avg / tot) * 100.0), "Average Score", title);
                }
            }
        }

        if (dataset.getRowCount() == 0) {
            showChartPlaceholder(scoreChartPanel, "No completed assessment results available.");
            return;
        }

        JFreeChart barChart = ChartFactory.createBarChart(
            null, "Assessment", "Average score (%)", dataset,
            PlotOrientation.VERTICAL, false, true, false
        );

        barChart.setBackgroundPaint(BG_CARD);

        CategoryPlot plot = barChart.getCategoryPlot();
        plot.setBackgroundPaint(BG_CARD);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(BORDER);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 11));
        domainAxis.setLabelPaint(TEXT_MUT);
        domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        domainAxis.setTickLabelPaint(TEXT_PRI);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0, 100);
        rangeAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 11));
        rangeAxis.setLabelPaint(TEXT_MUT);
        rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        rangeAxis.setTickLabelPaint(TEXT_PRI);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, ACCENT);
        renderer.setShadowVisible(false);

        ChartPanel cp = new ChartPanel(barChart);
        cp.setBackground(BG_CARD);
        cp.setPreferredSize(new Dimension(420, 230));
        replaceChartPanelContent(scoreChartPanel, cp);
    }

    // ── Completion Chart (JFreeChart Pie — dark themed) ───────────────────────

    @SuppressWarnings("unchecked")
    private void loadCompletionChart(Connection con) throws SQLException {
        String sql = "SELECT " +
                     "SUM(CASE WHEN UPPER(status) = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_count, " +
                     "SUM(CASE WHEN UPPER(status) <> 'COMPLETED' OR status IS NULL THEN 1 ELSE 0 END) AS other_count " +
                     "FROM exam_attempts";

        int completed = 0, other = 0;
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                completed = rs.getInt("completed_count");
                other     = rs.getInt("other_count");
            }
        }

        if (completed + other == 0) {
            showChartPlaceholder(completionChartPanel, "No exam attempts available.");
            return;
        }

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        dataset.setValue("Completed (" + completed + ")", completed);
        dataset.setValue("In Progress / Other (" + other + ")", other);

        JFreeChart pieChart = ChartFactory.createPieChart(null, dataset, true, true, false);
        pieChart.setBackgroundPaint(BG_CARD);

        PiePlot<String> plot = (PiePlot<String>) pieChart.getPlot();
        plot.setBackgroundPaint(BG_CARD);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);

        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
        plot.setLabelPaint(TEXT_PRI);
        plot.setLabelBackgroundPaint(BG_CARD2);
        plot.setLabelOutlinePaint(BORDER);

        if (pieChart.getLegend() != null) {
            pieChart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 11));
            pieChart.getLegend().setItemPaint(TEXT_PRI);
            pieChart.getLegend().setBackgroundPaint(BG_CARD);
        }

        plot.setSectionPaint("Completed (" + completed + ")", ACCENT);
        plot.setSectionPaint("In Progress / Other (" + other + ")", new Color(245, 158, 11));

        ChartPanel cp = new ChartPanel(pieChart);
        cp.setBackground(BG_CARD);
        cp.setPreferredSize(new Dimension(420, 230));
        replaceChartPanelContent(completionChartPanel, cp);
    }

    private void replaceChartPanelContent(JPanel card, JComponent newComponent) {
        Component north = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        card.removeAll();
        if (north != null) card.add(north, BorderLayout.NORTH);
        card.add(newComponent, BorderLayout.CENTER);
        card.revalidate();
        card.repaint();
    }

    private void showChartPlaceholder(JPanel card, String message) {
        JPanel ph = new JPanel(new GridBagLayout());
        ph.setBackground(BG_CARD);
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lbl.setForeground(TEXT_MUT);
        ph.add(lbl);
        replaceChartPanelContent(card, ph);
    }

    // ── Load Student Table & Search ────────────────────────────────────────────

    private void loadStudentTable(Connection con, String search) throws SQLException {
        tableModel.setRowCount(0);

        String sql = "SELECT u.user_id, u.full_name, " +
                     "COUNT(e.attempt_id) AS attempts, " +
                     "AVG(CASE WHEN UPPER(e.status) = 'COMPLETED' THEN e.score END) AS average_score, " +
                     "MAX(e.attempt_id) AS latest_attempt " +
                     "FROM users u " +
                     "LEFT JOIN exam_attempts e ON u.user_id = e.student_id " +
                     "WHERE u.role = 'Student' " +
                     "AND (u.username LIKE ? OR u.full_name LIKE ?) " +
                     "GROUP BY u.user_id, u.full_name ORDER BY u.user_id";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            String pattern = "%" + search + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                int serial = 0;
                while (rs.next()) {
                    String name     = rs.getString("full_name");
                    int    attempts = rs.getInt("attempts");
                    double avg      = rs.getDouble("average_score");
                    String avgStr   = rs.wasNull() ? "N/A" : String.format("%.2f", avg);
                    int    latestId = rs.getInt("latest_attempt");
                    String latestStr = "No attempts";
                    if (!rs.wasNull()) {
                        latestStr = getLatestResult(con, latestId);
                    }
                    tableModel.addRow(new Object[]{
                        String.valueOf(++serial), name, String.valueOf(attempts), avgStr, latestStr
                    });
                }
            }
        }
    }

    private String getLatestResult(Connection con, int attemptId) throws SQLException {
        String sql = "SELECT score, status FROM exam_attempts WHERE attempt_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double score  = rs.getDouble("score");
                    String status = rs.getString("status");
                    return String.format("%.2f (%s)", score, status == null ? "Unknown" : status);
                }
            }
        }
        return "N/A";
    }

    private void searchStudents() {
        String query = searchField.getText().trim();
        try (Connection con = DatabaseConnection.getConnection()) {
            loadStudentTable(con, query);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Leaderboards (Top Performers / Students Needing Assistance) ────────────

    private void loadLeaderboards(Connection con) throws SQLException {
        topList.removeAll();
        helpList.removeAll();

        String topSql =
            "SELECT u.full_name, COUNT(*) AS attempts, " +
            "AVG(e.score * 100.0 / e.total_marks) AS pct " +
            "FROM exam_attempts e JOIN users u ON u.user_id = e.student_id " +
            "WHERE UPPER(e.status) = 'COMPLETED' AND e.total_marks > 0 " +
            "GROUP BY u.user_id, u.full_name ORDER BY pct DESC LIMIT 5";
        try (PreparedStatement ps = con.prepareStatement(topSql);
             ResultSet rs = ps.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                double pct = rs.getDouble("pct");
                int n = rs.getInt("attempts");
                Color rankColor = rank == 1 ? DashboardUI.GOLD
                                : rank == 2 ? DashboardUI.SILVER
                                : rank == 3 ? DashboardUI.BRONZE : TEXT_MUT;
                topList.add(leaderRow("#" + rank, rankColor, rs.getString("full_name"),
                        n + (n == 1 ? " attempt" : " attempts"),
                        String.format("%.1f%%", pct), DashboardUI.EMERALD));
                rank++;
            }
            if (rank == 1) topList.add(DashboardUI.muted("No completed attempts yet."));
        }

        String helpSql =
            "SELECT u.full_name, a.title, (e.score * 100.0 / e.total_marks) AS pct " +
            "FROM exam_attempts e " +
            "JOIN users u ON u.user_id = e.student_id " +
            "JOIN assessments a ON a.assessment_id = e.assessment_id " +
            "WHERE UPPER(e.status) = 'COMPLETED' AND e.total_marks > 0 " +
            "AND (e.score * 100.0 / e.total_marks) < 50 " +
            "ORDER BY pct ASC LIMIT 5";
        try (PreparedStatement ps = con.prepareStatement(helpSql);
             ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                helpList.add(leaderRow("!", DashboardUI.DANGER, rs.getString("full_name"),
                        rs.getString("title"),
                        String.format("%.1f%%", rs.getDouble("pct")), DashboardUI.DANGER));
            }
            if (!any) helpList.add(DashboardUI.muted("No students below 50%."));
        }

        topList.revalidate();
        topList.repaint();
        helpList.revalidate();
        helpList.repaint();
    }

    private JComponent leaderRow(String lead, Color leadColor, String name, String detail,
                                 String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel rank = new JLabel(lead);
        rank.setFont(new Font("Segoe UI", Font.BOLD, 14));
        rank.setForeground(leadColor);
        rank.setPreferredSize(new Dimension(28, 20));

        JLabel nameLbl = new JLabel(name == null ? "Unknown" : name);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(TEXT_PRI);
        JLabel detailLbl = new JLabel(detail == null ? "" : detail);
        detailLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailLbl.setForeground(TEXT_MUT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(nameLbl);
        text.add(detailLbl);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(valueColor);

        row.add(rank, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // ── Export to CSV ──────────────────────────────────────────────────────────

    private void exportCsv() {
        if (tableModel.getRowCount() == 0) {
            ToastNotification.show(this, "No student data to export.", ToastNotification.Type.WARNING);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Student Performance");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File("student_performance.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }

        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write('﻿'); // BOM so Excel reads UTF-8 correctly
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                if (c > 0) sb.append(',');
                sb.append(csvEscape(tableModel.getColumnName(c)));
            }
            w.write(sb.append("\r\n").toString());
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) sb.append(',');
                    Object v = tableModel.getValueAt(r, c);
                    sb.append(csvEscape(v == null ? "" : v.toString()));
                }
                w.write(sb.append("\r\n").toString());
            }
            ToastNotification.show(this, "Exported to " + file.getName(), ToastNotification.Type.SUCCESS);
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csvEscape(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}