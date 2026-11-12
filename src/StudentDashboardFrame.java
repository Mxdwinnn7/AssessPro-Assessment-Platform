import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Student Dashboard — assessments, filters, availability, and calendar.
 */
public class StudentDashboardFrame extends JFrame {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final String studentName;
    private final List<AssessmentItem> assessments = new ArrayList<>();

    private String activeFilter = "All";
    private YearMonth visibleMonth = YearMonth.now();
    private LocalDate selectedDate;

    private JLabel examsValue;
    private JLabel completedValue;
    private JLabel pendingValue;
    private DashboardUI.RoundedCard performanceCard;
    private JPanel filterHost;
    private DashboardUI.Stack assessmentList;
    private JLabel monthLabel;
    private JPanel calendarGrid;
    private DashboardUI.Stack dayDetail;
    private DashboardUI.Stack deadlineList;

    public StudentDashboardFrame(String studentName) {
        super("AssessPro – Student Dashboard");
        this.studentName = studentName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 840);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        buildUI();
        loadAndRender();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(DashboardUI.BG_APP);
        setContentPane(root);
        root.add(DashboardUI.header("STUDENT PORTAL", () -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }), BorderLayout.NORTH);

        DashboardUI.Stack page = new DashboardUI.Stack();
        page.setBorder(new EmptyBorder(24, 28, 36, 28));

        JPanel split = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getMaximumSize() {
                Dimension preferred = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, preferred.height);
            }
        };
        split.setOpaque(false);
        split.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints leftC = new GridBagConstraints();
        leftC.gridx = 0;
        leftC.gridy = 0;
        leftC.weightx = 0.65;
        leftC.weighty = 0;
        leftC.fill = GridBagConstraints.HORIZONTAL;
        leftC.anchor = GridBagConstraints.NORTHWEST;
        leftC.insets = new Insets(0, 0, 0, 14);
        split.add(buildLeftColumn(), leftC);

        GridBagConstraints rightC = new GridBagConstraints();
        rightC.gridx = 1;
        rightC.gridy = 0;
        rightC.weightx = 0.35;
        rightC.weighty = 0;
        rightC.fill = GridBagConstraints.HORIZONTAL;
        rightC.anchor = GridBagConstraints.NORTHWEST;
        rightC.insets = new Insets(0, 10, 0, 0);
        split.add(buildRightColumn(), rightC);

        page.add(split);
        root.add(DashboardUI.scroll(page), BorderLayout.CENTER);
    }

    private JComponent buildLeftColumn() {
        DashboardUI.Stack column = new DashboardUI.Stack();

        JLabel welcome = new JLabel("Welcome, " + fetchDisplayName() + "!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(DashboardUI.TEXT_PRI);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel message = DashboardUI.muted("Ready for your next assessment?");
        column.add(welcome);
        column.add(Box.createVerticalStrut(4));
        column.add(message);
        column.add(Box.createVerticalStrut(18));

        examsValue = statValue();
        completedValue = statValue();
        pendingValue = statValue();
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 108));
        stats.add(statCard("MY EXAMS", examsValue, DashboardUI.ACCENT));
        stats.add(statCard("COMPLETED", completedValue, DashboardUI.EMERALD));
        stats.add(statCard("PENDING", pendingValue, DashboardUI.AMBER));
        column.add(stats);
        column.add(Box.createVerticalStrut(16));

        performanceCard = new DashboardUI.RoundedCard().accent(DashboardUI.EMERALD).hoverable();
        performanceCard.setLayout(new BoxLayout(performanceCard, BoxLayout.Y_AXIS));
        column.add(performanceCard);
        column.add(Box.createVerticalStrut(16));

        DashboardUI.QuietButton resultsBtn = new DashboardUI.QuietButton("My Results");
        resultsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultsBtn.addActionListener(e -> {
            dispose();
            new StudentResultsHistoryFrame(studentName).setVisible(true);
        });
        column.add(resultsBtn);
        column.add(Box.createVerticalStrut(22));
        column.add(DashboardUI.sectionTitle("Available Assessments"));
        column.add(Box.createVerticalStrut(12));

        filterHost = new JPanel(new BorderLayout());
        filterHost.setOpaque(false);
        filterHost.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(filterHost);
        column.add(Box.createVerticalStrut(14));

        assessmentList = new DashboardUI.Stack();
        column.add(assessmentList);
        return column;
    }

    private JComponent buildRightColumn() {
        DashboardUI.Stack column = new DashboardUI.Stack();

        DashboardUI.RoundedCard calendarCard = new DashboardUI.RoundedCard();
        calendarCard.setLayout(new BoxLayout(calendarCard, BoxLayout.Y_AXIS));

        JLabel calendarTitle = DashboardUI.sectionTitle("Assessment Calendar");
        calendarCard.add(calendarTitle);
        calendarCard.add(Box.createVerticalStrut(12));

        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        monthLabel.setForeground(DashboardUI.TEXT_PRI);

        DashboardUI.QuietButton prev = new DashboardUI.QuietButton("<");
        DashboardUI.QuietButton next = new DashboardUI.QuietButton(">");
        prev.addActionListener(e -> {
            visibleMonth = visibleMonth.minusMonths(1);
            renderCalendar();
        });
        next.addActionListener(e -> {
            visibleMonth = visibleMonth.plusMonths(1);
            renderCalendar();
        });

        JPanel nav = new JPanel(new BorderLayout(8, 0));
        nav.setOpaque(false);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        nav.add(prev, BorderLayout.WEST);
        nav.add(monthLabel, BorderLayout.CENTER);
        nav.add(next, BorderLayout.EAST);
        calendarCard.add(nav);
        calendarCard.add(Box.createVerticalStrut(10));

        JPanel weekdays = new JPanel(new GridLayout(1, 7, 4, 0));
        weekdays.setOpaque(false);
        weekdays.setAlignmentX(Component.LEFT_ALIGNMENT);
        weekdays.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        for (String day : new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}) {
            JLabel label = new JLabel(day, SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setForeground(DashboardUI.TEXT_MUT);
            weekdays.add(label);
        }
        calendarCard.add(weekdays);
        calendarCard.add(Box.createVerticalStrut(6));

        calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));
        calendarGrid.setOpaque(false);
        calendarGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        calendarCard.add(calendarGrid);
        calendarCard.add(Box.createVerticalStrut(14));

        dayDetail = new DashboardUI.Stack();
        calendarCard.add(dayDetail);
        column.add(calendarCard);
        column.add(Box.createVerticalStrut(14));

        DashboardUI.RoundedCard deadlines = new DashboardUI.RoundedCard().accent(DashboardUI.AMBER);
        deadlines.setLayout(new BoxLayout(deadlines, BoxLayout.Y_AXIS));
        deadlines.add(DashboardUI.sectionTitle("Upcoming Deadlines"));
        deadlines.add(Box.createVerticalStrut(10));
        deadlineList = new DashboardUI.Stack();
        deadlines.add(deadlineList);
        column.add(deadlines);
        return column;
    }

    private void loadAndRender() {
        assessments.clear();
        assessments.addAll(loadAssessments());
        int totalExams = fetchCount("SELECT COUNT(*) FROM assessments");
        int completedCount = fetchCompletedCount();
        examsValue.setText(String.format("%02d", totalExams));
        completedValue.setText(String.format("%02d", completedCount));
        pendingValue.setText(String.format("%02d", Math.max(0, totalExams - completedCount)));
        fillPerformance();
        performanceCard.bindInteraction();
        renderFilters();
        renderAssessments();
        renderCalendar();
        renderDeadlines();
    }

    private void renderFilters() {
        filterHost.removeAll();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        addFilter(row, "All", "All");
        addFilter(row, "Java", "Java");
        addFilter(row, "Databases", "Databases");
        addFilter(row, "Web", "Web Development");
        addFilter(row, "Completed", "Completed");
        filterHost.add(row, BorderLayout.CENTER);
        filterHost.revalidate();
        filterHost.repaint();
    }

    private void addFilter(JPanel row, String id, String label) {
        DashboardUI.PillButton tab = new DashboardUI.PillButton(label + "  " + countFor(id), id.equals(activeFilter));
        tab.addActionListener(e -> {
            activeFilter = id;
            renderFilters();
            renderAssessments();
        });
        row.add(tab);
    }

    private int countFor(String id) {
        int count = 0;
        for (AssessmentItem item : assessments) {
            if (matches(item, id)) count++;
        }
        return count;
    }

    private boolean matches(AssessmentItem item, String filter) {
        String subject = item.subject == null ? "" : item.subject.toLowerCase();
        switch (filter) {
            case "Java":
                return subject.contains("java");
            case "Databases":
                return subject.contains("database");
            case "Web":
                return subject.contains("web");
            case "Completed":
                return item.completed;
            default:
                return true;
        }
    }

    private void renderAssessments() {
        assessmentList.removeAll();
        List<AssessmentItem> shown = new ArrayList<>();
        for (AssessmentItem item : assessments) {
            if (matches(item, activeFilter)) shown.add(item);
        }
        if (shown.isEmpty()) {
            assessmentList.add(emptyCard(assessments.isEmpty()
                    ? "No assessments available yet."
                    : "No assessments match this filter."));
        } else {
            for (int i = 0; i < shown.size(); i++) {
                assessmentList.add(buildAssessmentCard(shown.get(i)));
                if (i < shown.size() - 1) assessmentList.add(Box.createVerticalStrut(12));
            }
        }
        assessmentList.revalidate();
        assessmentList.repaint();
    }

    private DashboardUI.RoundedCard buildAssessmentCard(AssessmentItem item) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().hoverable();
        Availability status = statusOf(item);
        boolean windowOpen = isWindowOpen(item);
        boolean canStart = windowOpen && item.actualQuestions > 0;

        JLabel title = new JLabel(item.title == null ? "Assessment" : item.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(DashboardUI.TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subject = new JLabel(item.subject == null ? "" : item.subject);
        subject.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subject.setForeground(DashboardUI.ACCENT);
        subject.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel meta = DashboardUI.muted(item.durationMinutes + " min   ·   " + item.actualQuestions + " questions");
        JLabel extra = DashboardUI.muted(statusDetail(item, status));

        DashboardUI.Stack info = new DashboardUI.Stack();
        info.add(title);
        info.add(Box.createVerticalStrut(4));
        info.add(subject);
        info.add(Box.createVerticalStrut(8));
        info.add(statusBadge(status));
        info.add(Box.createVerticalStrut(8));
        info.add(meta);
        info.add(Box.createVerticalStrut(2));
        info.add(extra);

        DashboardUI.PrimaryButton start = new DashboardUI.PrimaryButton("Start Exam");
        start.setEnabled(canStart);
        if (!canStart) {
            start.setToolTipText(startBlockedReason(item, status));
        } else if (item.completed) {
            start.setToolTipText("You have already attempted this assessment. You can start it again while it is open.");
        }
        start.addActionListener(e -> {
            dispose();
            new ExamFrame(studentName, item.id, item.title, item.durationMinutes).setVisible(true);
        });

        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(start);

        card.add(info, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        card.bindInteraction();
        return card;
    }

    private String statusDetail(AssessmentItem item, Availability status) {
        switch (status) {
            case UPCOMING:
                return item.availableFrom == null ? "Not open yet" : "Opens " + item.availableFrom.format(DATE_FMT);
            case EXPIRED:
                return item.availableUntil == null ? "The assessment window has closed."
                        : "Window closed " + item.availableUntil.format(DATE_FMT);
            case COMPLETED:
                return isWindowOpen(item) ? "You have already attempted this assessment."
                        : "Completed. The assessment window has closed.";
            default:
                return item.actualQuestions > 0 ? "You can start this assessment now."
                        : "No questions have been added yet.";
        }
    }

    private String startBlockedReason(AssessmentItem item, Availability status) {
        if (item.actualQuestions <= 0 && isWindowOpen(item)) {
            return "No questions added to this assessment yet.";
        }
        if (status == Availability.UPCOMING && item.availableFrom != null) {
            return "Opens " + item.availableFrom.format(DATE_FMT);
        }
        if (!isWindowOpen(item)) return "This assessment window has closed.";
        return "This assessment cannot be started right now.";
    }

    private JLabel statusBadge(Availability status) {
        switch (status) {
            case COMPLETED:
                return DashboardUI.badge("Completed", DashboardUI.EMERALD);
            case UPCOMING:
                return DashboardUI.badge("Upcoming", DashboardUI.AMBER);
            case EXPIRED:
                return DashboardUI.badge("Expired", DashboardUI.DANGER);
            default:
                return DashboardUI.badge("Available Now", DashboardUI.ACCENT);
        }
    }

    private void fillPerformance() {
        performanceCard.removeAll();
        LastResult last = fetchLastResult();
        JLabel eyebrow = new JLabel("RECENT PERFORMANCE");
        eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
        eyebrow.setForeground(DashboardUI.TEXT_MUT);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        performanceCard.add(eyebrow);
        performanceCard.add(Box.createVerticalStrut(8));

        if (last == null) {
            performanceCard.add(DashboardUI.muted("You have not completed an assessment yet."));
            return;
        }

        JLabel title = new JLabel(last.title);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(DashboardUI.TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        int pct = (int) Math.round(last.percentage);
        Color tone = last.percentage >= 50 ? DashboardUI.EMERALD : DashboardUI.DANGER;
        JLabel score = new JLabel("Score " + last.score + " / " + last.totalMarks
                + "    ·    " + pct + "%    ·    Grade " + DashboardUI.letterGrade(last.percentage));
        score.setFont(DashboardUI.UI_BOLD);
        score.setForeground(tone);
        score.setAlignmentX(Component.LEFT_ALIGNMENT);

        performanceCard.add(title);
        performanceCard.add(Box.createVerticalStrut(6));
        performanceCard.add(score);
        performanceCard.add(Box.createVerticalStrut(10));
        performanceCard.add(new DashboardUI.ProgressTrack(pct, tone));
    }

    private void renderCalendar() {
        monthLabel.setText(visibleMonth.format(MONTH_FMT));
        calendarGrid.removeAll();
        LocalDate first = visibleMonth.atDay(1);
        int lead = first.getDayOfWeek().getValue() % 7;
        int days = visibleMonth.lengthOfMonth();
        int cells = lead + days;
        int total = (int) (Math.ceil(cells / 7.0) * 7);
        LocalDate today = LocalDate.now();

        for (int i = 0; i < total; i++) {
            if (i < lead || i >= lead + days) {
                calendarGrid.add(new DayCell(null, null, false, false));
            } else {
                LocalDate date = first.plusDays(i - lead);
                List<AssessmentItem> onDay = assessmentsOn(date);
                calendarGrid.add(new DayCell(date, dotColor(onDay), date.equals(today), date.equals(selectedDate)));
            }
        }
        calendarGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, calendarGrid.getPreferredSize().height));
        calendarGrid.revalidate();
        calendarGrid.repaint();
        renderDayDetail();
    }

    private void renderDayDetail() {
        dayDetail.removeAll();
        if (selectedDate == null) {
            dayDetail.add(DashboardUI.muted("Select a date to see scheduled assessments."));
        } else {
            JLabel heading = new JLabel(selectedDate.format(DAY_FMT));
            heading.setFont(new Font("Segoe UI", Font.BOLD, 14));
            heading.setForeground(DashboardUI.TEXT_PRI);
            heading.setAlignmentX(Component.LEFT_ALIGNMENT);
            dayDetail.add(heading);
            dayDetail.add(Box.createVerticalStrut(8));
            List<AssessmentItem> items = assessmentsOn(selectedDate);
            if (items.isEmpty()) {
                dayDetail.add(DashboardUI.muted("No assessments on this date."));
            } else {
                for (int i = 0; i < items.size(); i++) {
                    dayDetail.add(dayItem(items.get(i)));
                    if (i < items.size() - 1) dayDetail.add(Box.createVerticalStrut(8));
                }
            }
        }
        dayDetail.revalidate();
        dayDetail.repaint();
    }

    private JComponent dayItem(AssessmentItem item) {
        DashboardUI.Stack block = new DashboardUI.Stack();
        JLabel title = new JLabel(item.title == null ? "Assessment" : item.title);
        title.setFont(DashboardUI.UI_BOLD);
        title.setForeground(DashboardUI.TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.add(title);
        block.add(DashboardUI.muted("Subject: " + (item.subject == null ? "—" : item.subject)));
        block.add(DashboardUI.muted("Opens: " + formatWhen(item.availableFrom)));
        block.add(DashboardUI.muted("Closes: " + formatWhen(item.availableUntil)));
        block.add(Box.createVerticalStrut(4));
        block.add(statusBadge(statusOf(item)));
        return block;
    }

    private void renderDeadlines() {
        deadlineList.removeAll();
        List<AssessmentItem> upcoming = new ArrayList<>();
        for (AssessmentItem item : assessments) {
            Availability status = statusOf(item);
            if (status == Availability.AVAILABLE || status == Availability.UPCOMING) upcoming.add(item);
        }
        upcoming.sort((a, b) -> scheduleKey(a).compareTo(scheduleKey(b)));
        if (upcoming.isEmpty()) {
            deadlineList.add(DashboardUI.muted("No open or upcoming assessments."));
        } else {
            int limit = Math.min(3, upcoming.size());
            for (int i = 0; i < limit; i++) {
                deadlineList.add(deadlineRow(upcoming.get(i)));
                if (i < limit - 1) deadlineList.add(Box.createVerticalStrut(10));
            }
        }
        deadlineList.revalidate();
        deadlineList.repaint();
    }

    private JComponent deadlineRow(AssessmentItem item) {
        DashboardUI.Stack row = new DashboardUI.Stack();
        JLabel title = new JLabel(item.title == null ? "Assessment" : item.title);
        title.setFont(DashboardUI.UI_BOLD);
        title.setForeground(DashboardUI.TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        LocalDateTime when = item.availableUntil != null ? item.availableUntil : item.availableFrom;
        String whenText = when == null ? "Open now" : when.format(DATE_FMT);
        row.add(title);
        row.add(DashboardUI.muted((item.subject == null ? "Assessment" : item.subject) + "  ·  " + whenText));
        row.add(DashboardUI.muted(deadlineHint(item)));
        return row;
    }

    private String deadlineHint(AssessmentItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (statusOf(item) == Availability.UPCOMING && item.availableFrom != null) {
            return "Opens in " + DashboardUI.formatRemaining(Duration.between(now, item.availableFrom));
        }
        if (item.availableUntil != null && now.isBefore(item.availableUntil)) {
            return "Closes in " + DashboardUI.formatRemaining(Duration.between(now, item.availableUntil));
        }
        return "Open";
    }

    private LocalDateTime scheduleKey(AssessmentItem item) {
        if (statusOf(item) == Availability.UPCOMING && item.availableFrom != null) return item.availableFrom;
        if (item.availableUntil != null) return item.availableUntil;
        if (item.availableFrom != null) return item.availableFrom;
        return LocalDateTime.MAX;
    }

    private List<AssessmentItem> assessmentsOn(LocalDate day) {
        List<AssessmentItem> list = new ArrayList<>();
        for (AssessmentItem item : assessments) {
            if (covers(item, day)) list.add(item);
        }
        return list;
    }

    private boolean covers(AssessmentItem item, LocalDate day) {
        LocalDate from = item.availableFrom == null ? null : item.availableFrom.toLocalDate();
        LocalDate until = item.availableUntil == null ? null : item.availableUntil.toLocalDate();
        if (from == null && until == null) return false;
        if (from != null && until != null) return !day.isBefore(from) && !day.isAfter(until);
        return day.equals(from != null ? from : until);
    }

    private Color dotColor(List<AssessmentItem> items) {
        if (items.isEmpty()) return null;
        boolean upcoming = false;
        boolean completed = false;
        boolean expired = false;
        for (AssessmentItem item : items) {
            switch (statusOf(item)) {
                case AVAILABLE:
                    return DashboardUI.ACCENT;
                case UPCOMING:
                    upcoming = true;
                    break;
                case COMPLETED:
                    completed = true;
                    break;
                case EXPIRED:
                    expired = true;
                    break;
                default:
                    break;
            }
        }
        if (upcoming) return DashboardUI.AMBER;
        if (completed) return DashboardUI.EMERALD;
        if (expired) return DashboardUI.DANGER;
        return DashboardUI.ACCENT;
    }

    private Availability statusOf(AssessmentItem item) {
        if (item.completed) return Availability.COMPLETED;
        LocalDateTime now = LocalDateTime.now();
        if (item.availableFrom != null && now.isBefore(item.availableFrom)) return Availability.UPCOMING;
        if (item.availableUntil != null && now.isAfter(item.availableUntil)) return Availability.EXPIRED;
        return Availability.AVAILABLE;
    }

    private boolean isWindowOpen(AssessmentItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (item.availableFrom != null && now.isBefore(item.availableFrom)) return false;
        if (item.availableUntil != null && now.isAfter(item.availableUntil)) return false;
        return true;
    }

    private String formatWhen(LocalDateTime time) {
        return time == null ? "Not set" : time.format(DATE_FMT);
    }

    private JLabel statValue() {
        JLabel label = new JLabel("–");
        label.setFont(new Font("Segoe UI", Font.BOLD, 26));
        label.setForeground(DashboardUI.TEXT_PRI);
        return label;
    }

    private JComponent statCard(String heading, JLabel value, Color accent) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard().accent(accent).hoverable();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setForeground(DashboardUI.TEXT_MUT);
        h.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(h);
        card.add(Box.createVerticalStrut(6));
        card.add(value);
        card.bindInteraction();
        return card;
    }

    private JComponent emptyCard(String message) {
        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard();
        card.add(DashboardUI.muted(message), BorderLayout.CENTER);
        return card;
    }

    private String fetchDisplayName() {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT full_name FROM users WHERE username = ?")) {
            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("full_name");
                    if (name != null && !name.isBlank()) return name;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return studentName;
    }

    private int fetchCount(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private int fetchCompletedCount() {
        String sql = "SELECT COUNT(*) FROM exam_attempts ea " +
                "JOIN users u ON ea.student_id = u.user_id " +
                "WHERE u.username = ? AND UPPER(ea.status) = 'COMPLETED'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private List<AssessmentItem> loadAssessments() {
        List<AssessmentItem> list = new ArrayList<>();
        String sql = "SELECT a.assessment_id, a.title, a.subject, a.duration_minutes, " +
                "a.available_from, a.available_until, COUNT(q.question_id) AS actual_questions, " +
                "EXISTS (SELECT 1 FROM exam_attempts ea JOIN users u ON ea.student_id = u.user_id " +
                "WHERE ea.assessment_id = a.assessment_id AND u.username = ? AND UPPER(ea.status) = 'COMPLETED') AS completed " +
                "FROM assessments a LEFT JOIN questions q ON a.assessment_id = q.assessment_id " +
                "GROUP BY a.assessment_id, a.title, a.subject, a.duration_minutes, a.available_from, a.available_until " +
                "ORDER BY a.assessment_id DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp from = rs.getTimestamp("available_from");
                    Timestamp until = rs.getTimestamp("available_until");
                    list.add(new AssessmentItem(
                            rs.getInt("assessment_id"),
                            rs.getString("title"),
                            rs.getString("subject"),
                            rs.getInt("duration_minutes"),
                            rs.getInt("actual_questions"),
                            from == null ? null : from.toLocalDateTime(),
                            until == null ? null : until.toLocalDateTime(),
                            rs.getBoolean("completed")));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load assessments.\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return list;
    }

    private LastResult fetchLastResult() {
        String sql = "SELECT a.title, ea.score, ea.total_marks FROM exam_attempts ea " +
                "JOIN assessments a ON ea.assessment_id = a.assessment_id " +
                "JOIN users u ON ea.student_id = u.user_id " +
                "WHERE u.username = ? AND UPPER(ea.status) = 'COMPLETED' " +
                "ORDER BY ea.submitted_at DESC, ea.attempt_id DESC LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, studentName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int score = rs.getInt("score");
                    int total = rs.getInt("total_marks");
                    double pct = total > 0 ? (score * 100.0) / total : 0;
                    String title = rs.getString("title");
                    return new LastResult(title == null ? "Assessment" : title, score, total, pct);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private enum Availability { AVAILABLE, UPCOMING, EXPIRED, COMPLETED }

    private final class DayCell extends JPanel {
        private final LocalDate date;
        private final Color dot;
        private final boolean today;
        private final boolean selected;
        private boolean hovered;

        DayCell(LocalDate date, Color dot, boolean today, boolean selected) {
            this.date = date;
            this.dot = dot;
            this.today = today;
            this.selected = selected;
            setOpaque(false);
            setPreferredSize(new Dimension(40, 42));
            if (date != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }

                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectedDate = date;
                        renderCalendar();
                    }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (date == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 12;
            if (selected) {
                g2.setColor(DashboardUI.ACCENT);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, arc, arc);
            } else if (hovered) {
                g2.setColor(DashboardUI.BG_CARD2);
                g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, arc, arc);
            }
            if (today && !selected) {
                g2.setColor(DashboardUI.ACCENT);
                g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, arc, arc);
            }
            g2.setFont(new Font("Segoe UI", selected ? Font.BOLD : Font.PLAIN, 12));
            g2.setColor(selected ? Color.WHITE : DashboardUI.TEXT_PRI);
            String text = String.valueOf(date.getDayOfMonth());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, 18);
            if (dot != null) {
                g2.setColor(selected ? Color.WHITE : dot);
                g2.fillOval(getWidth() / 2 - 3, getHeight() - 12, 6, 6);
            }
            g2.dispose();
        }
    }

    private static class AssessmentItem {
        final int id;
        final int durationMinutes;
        final int actualQuestions;
        final String title;
        final String subject;
        final LocalDateTime availableFrom;
        final LocalDateTime availableUntil;
        final boolean completed;

        AssessmentItem(int id, String title, String subject, int durationMinutes, int actualQuestions,
                       LocalDateTime availableFrom, LocalDateTime availableUntil, boolean completed) {
            this.id = id;
            this.title = title;
            this.subject = subject;
            this.durationMinutes = durationMinutes;
            this.actualQuestions = actualQuestions;
            this.availableFrom = availableFrom;
            this.availableUntil = availableUntil;
            this.completed = completed;
        }
    }

    private static class LastResult {
        final String title;
        final int score;
        final int totalMarks;
        final double percentage;

        LastResult(String title, int score, int totalMarks, double percentage) {
            this.title = title;
            this.score = score;
            this.totalMarks = totalMarks;
            this.percentage = percentage;
        }
    }
}
