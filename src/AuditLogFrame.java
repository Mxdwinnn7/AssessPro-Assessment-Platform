import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audit Log screen — a searchable, filterable history of admin actions.
 */
public class AuditLogFrame extends JFrame {

    /** Friendly label and colour for each action code written by AuditLog.log(). */
    private static final Map<String, String> ACTION_LABELS = new LinkedHashMap<>();
    static {
        ACTION_LABELS.put("CREATE_ASSESSMENT", "Assessment created");
        ACTION_LABELS.put("ADD_QUESTION",      "Question added");
        ACTION_LABELS.put("UPDATE_SETTINGS",   "Settings updated");
        ACTION_LABELS.put("DELETE_ASSESSMENT", "Assessment deleted");
        ACTION_LABELS.put("CREATE_USER",       "User created");
        ACTION_LABELS.put("DELETE_USER",       "User deleted");
    }

    /** Friendly text for an action code (used by the dashboard activity feed too). */
    public static String labelFor(String code) {
        return ACTION_LABELS.getOrDefault(code, code);
    }

    private final String adminName;
    private DefaultTableModel model;
    private JTextField searchField;
    private JComboBox<String> actionFilter;
    private JLabel countLabel;

    public AuditLogFrame(String adminName) {
        super("AssessPro – Audit Log");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        buildUI();
        loadLog();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(DashboardUI.BG_APP);
        setContentPane(root);
        root.add(AdminUI.subHeader("AUDIT LOG", () -> {
            dispose();
            new AdminDashboardFrame(adminName).setVisible(true);
        }), BorderLayout.NORTH);

        DashboardUI.Stack content = new DashboardUI.Stack();
        content.setBorder(new EmptyBorder(24, 32, 32, 32));

        JLabel heading = new JLabel("Audit Log");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(DashboardUI.TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(heading);
        content.add(Box.createVerticalStrut(4));
        content.add(DashboardUI.muted("A record of who changed what, and when. Newest first."));
        content.add(Box.createVerticalStrut(18));

        DashboardUI.RoundedCard card = new DashboardUI.RoundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = new JTextField(20);
        UIUtils.styleTextField(searchField, 7, 10, 7, 10);
        searchField.addActionListener(e -> loadLog());

        DashboardUI.PrimaryButton searchBtn = new DashboardUI.PrimaryButton("Search");
        searchBtn.addActionListener(e -> loadLog());

        String[] filterItems = new String[ACTION_LABELS.size() + 1];
        filterItems[0] = "All actions";
        int i = 1;
        for (String label : ACTION_LABELS.values()) filterItems[i++] = label;
        actionFilter = new JComboBox<>(filterItems);
        AdminUI.flatCombo(actionFilter);
        actionFilter.setPreferredSize(new Dimension(190, 34));
        actionFilter.addActionListener(e -> loadLog());

        DashboardUI.QuietButton refresh = new DashboardUI.QuietButton("Refresh");
        refresh.addActionListener(e -> {
            searchField.setText("");
            actionFilter.setSelectedIndex(0);
            loadLog();
        });

        DashboardUI.QuietButton export = new DashboardUI.QuietButton("Export to CSV");
        export.addActionListener(e -> AdminUI.exportCsv(this, model, "Export Audit Log", "audit_log.csv"));

        bar.add(searchField);
        bar.add(searchBtn);
        bar.add(actionFilter);
        bar.add(refresh);
        bar.add(export);
        card.add(bar);
        card.add(Box.createVerticalStrut(14));

        model = new DefaultTableModel(new String[]{"Time", "Admin", "Action", "Details"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = AdminUI.darkTable(model);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(520);
        AdminUI.StripedRenderer renderer = new AdminUI.StripedRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (col == 2 && v != null) {
                    String s = v.toString();
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    if (s.contains("deleted"))      c.setForeground(DashboardUI.DANGER);
                    else if (s.contains("created") || s.contains("added")) c.setForeground(DashboardUI.EMERALD);
                    else                            c.setForeground(DashboardUI.AMBER);
                }
                c.setToolTipText(col == 3 && v != null ? v.toString() : null);
                return c;
            }
        };
        for (int c = 0; c < table.getColumnCount(); c++) table.getColumnModel().getColumn(c).setCellRenderer(renderer);

        JScrollPane scroll = AdminUI.tableScroll(table);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(0, 420));
        card.add(scroll);
        card.add(Box.createVerticalStrut(10));

        countLabel = DashboardUI.muted(" ");
        card.add(countLabel);

        content.add(card);
        root.add(DashboardUI.scroll(content), BorderLayout.CENTER);
    }

    private void loadLog() {
        model.setRowCount(0);
        AuditLog.ensureTable();

        String search = searchField.getText().trim();
        int sel = actionFilter.getSelectedIndex();
        String actionCode = null;
        if (sel > 0) {
            int i = 1;
            for (String code : ACTION_LABELS.keySet()) {
                if (i++ == sel) { actionCode = code; break; }
            }
        }

        StringBuilder sql = new StringBuilder(
                "SELECT created_at, actor, action, details FROM audit_log WHERE 1=1 ");
        if (!search.isEmpty()) sql.append("AND (actor LIKE ? OR details LIKE ?) ");
        if (actionCode != null) sql.append("AND action = ? ");
        sql.append("ORDER BY created_at DESC, log_id DESC LIMIT 500");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int p = 1;
            if (!search.isEmpty()) {
                String pattern = "%" + search + "%";
                ps.setString(p++, pattern);
                ps.setString(p++, pattern);
            }
            if (actionCode != null) ps.setString(p, actionCode);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("action");
                    model.addRow(new Object[]{
                        rs.getTimestamp("created_at").toLocalDateTime()
                          .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm:ss")),
                        rs.getString("actor"),
                        ACTION_LABELS.getOrDefault(code, code),
                        rs.getString("details")
                    });
                }
            }
            int n = model.getRowCount();
            countLabel.setText(n == 0 ? "No log entries match." :
                    n + (n == 1 ? " entry" : " entries") + (n == 500 ? " (showing the latest 500)" : ""));
        } catch (SQLException ex) {
            ex.printStackTrace();
            countLabel.setText("Could not load the audit log: " + ex.getMessage());
        }
    }
}
