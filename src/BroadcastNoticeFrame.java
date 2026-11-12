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

/**
 * Broadcast Notice — Admin tool to post, toggle and delete notices that
 * appear as banners on the Student Dashboard.
 *
 * Database table is auto-created on first use.
 */
public class BroadcastNoticeFrame extends JFrame {

    private static final Color BG_APP   = DashboardUI.BG_APP;
    private static final Color BG_CARD  = DashboardUI.BG_CARD;
    private static final Color BG_CARD2 = DashboardUI.BG_CARD2;
    private static final Color TEXT_PRI = DashboardUI.TEXT_PRI;
    private static final Color TEXT_MUT = DashboardUI.TEXT_MUT;
    private static final Color BORDER   = DashboardUI.BORDER;
    private static final Color ACCENT   = DashboardUI.ACCENT;
    private static final Color EMERALD  = DashboardUI.EMERALD;
    private static final Color DANGER   = DashboardUI.DANGER;
    private static final Color AMBER    = DashboardUI.AMBER;

    private final String adminName;
    private DefaultTableModel tableModel;
    private JTable table;
    private int hoverRow = -1;

    private JTextField titleField;
    private JTextArea  messageArea;
    private JComboBox<String> typeBox;

    // ── Notice type colour mapping ───────────────────────────────────────────
    public static final String TYPE_INFO    = "Info";
    public static final String TYPE_WARNING = "Warning";
    public static final String TYPE_URGENT  = "Urgent";

    public BroadcastNoticeFrame(String adminName) {
        super("AssessPro – Broadcast Notices");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 780);
        setMinimumSize(new Dimension(900, 620));
        setLocationRelativeTo(null);
        ensureTable();
        buildUI();
        loadNotices();
    }

    // ── Auto-create notices table ────────────────────────────────────────────

    public static void ensureTable() {
        String ddl =
            "CREATE TABLE IF NOT EXISTS notices (" +
            "  notice_id  INT AUTO_INCREMENT PRIMARY KEY, " +
            "  title      VARCHAR(200) NOT NULL, " +
            "  message    TEXT NOT NULL, " +
            "  type       VARCHAR(20) NOT NULL DEFAULT 'Info', " +
            "  is_active  TINYINT(1) NOT NULL DEFAULT 1, " +
            "  created_by VARCHAR(100) NOT NULL, " +
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute(ddl);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_APP);
        setContentPane(root);

        root.add(AdminUI.subHeader("BROADCAST NOTICES", () -> {
            dispose();
            new AdminDashboardFrame(adminName).setVisible(true);
        }), BorderLayout.NORTH);

        DashboardUI.Stack page = new DashboardUI.Stack();
        page.setBorder(new EmptyBorder(26, 34, 36, 34));

        // Heading
        JLabel heading = new JLabel("Broadcast Notices");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = DashboardUI.muted("Post notices that appear as banners on every student's dashboard.");
        page.add(heading);
        page.add(Box.createVerticalStrut(4));
        page.add(sub);
        page.add(Box.createVerticalStrut(24));

        // ── Compose card ─────────────────────────────────────────────────────
        page.add(DashboardUI.sectionTitle("Post a New Notice"));
        page.add(Box.createVerticalStrut(12));

        DashboardUI.RoundedCard composeCard = new DashboardUI.RoundedCard();
        composeCard.setLayout(new BoxLayout(composeCard, BoxLayout.Y_AXIS));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        titleField = new JTextField();
        titleField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        UIUtils.styleTextField(titleField, 8, 12, 8, 12);
        titleField.putClientProperty("JTextField.placeholderText", "Notice title...");

        typeBox = new JComboBox<>(new String[]{TYPE_INFO, TYPE_WARNING, TYPE_URGENT});
        AdminUI.flatCombo(typeBox);
        typeBox.setPreferredSize(new Dimension(130, 36));
        typeBox.setMaximumSize(new Dimension(130, 36));

        JPanel typeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        typeWrap.setOpaque(false);
        typeWrap.setPreferredSize(new Dimension(145, 36));
        typeWrap.add(typeBox);

        titleRow.add(fieldLabel("TITLE"), BorderLayout.NORTH);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Restructure as vertical stack inside compose
        JPanel titleWrap = new JPanel(new GridBagLayout());
        titleWrap.setOpaque(false);
        titleWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        titleWrap.add(titleField, gc);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(0, 10, 0, 0);
        titleWrap.add(typeBox, gc);

        composeCard.add(fieldLabel("TITLE"));
        composeCard.add(Box.createVerticalStrut(5));
        composeCard.add(titleWrap);
        composeCard.add(Box.createVerticalStrut(12));
        composeCard.add(fieldLabel("MESSAGE"));
        composeCard.add(Box.createVerticalStrut(5));

        messageArea = new JTextArea(4, 0);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        messageArea.setBackground(BG_CARD2);
        messageArea.setForeground(TEXT_PRI);
        messageArea.setCaretColor(TEXT_PRI);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(new EmptyBorder(8, 12, 8, 12));
        JScrollPane msgScroll = new JScrollPane(messageArea);
        msgScroll.setBorder(new LineBorder(BORDER, 1, true));
        msgScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        composeCard.add(msgScroll);
        composeCard.add(Box.createVerticalStrut(14));

        // Post button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        DashboardUI.PrimaryButton postBtn = new DashboardUI.PrimaryButton("📢  Post Notice");
        postBtn.addActionListener(e -> postNotice());
        DashboardUI.QuietButton clearBtn = new DashboardUI.QuietButton("Clear");
        clearBtn.addActionListener(e -> { titleField.setText(""); messageArea.setText(""); });
        btnRow.add(postBtn);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(clearBtn);
        composeCard.add(btnRow);

        page.add(composeCard);
        page.add(Box.createVerticalStrut(26));

        // ── Existing notices table ────────────────────────────────────────────
        page.add(DashboardUI.sectionTitle("Posted Notices"));
        page.add(Box.createVerticalStrut(12));

        DashboardUI.RoundedCard tableCard = new DashboardUI.RoundedCard();
        tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS));

        // Hint
        JLabel hint = DashboardUI.muted("Click a notice row to toggle its Active / Inactive status. Use the Delete button to remove it.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(hint);
        tableCard.add(Box.createVerticalStrut(10));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Type", "Title", "Message", "Status", "Posted By", "Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT_PRI);
        table.setBackground(BG_CARD);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER);
        table.setSelectionBackground(BG_CARD2);
        table.setSelectionForeground(TEXT_PRI);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(320);
        table.getColumnModel().getColumn(4).setMaxWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(140);

        // Hover
        table.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoverRow) { hoverRow = row; table.repaint(); }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { hoverRow = -1; table.repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row >= 0) toggleStatus(row);
            }
        });

        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(BG_CARD2);
        th.setForeground(TEXT_MUT);
        th.setPreferredSize(new Dimension(0, 36));
        ((DefaultTableCellRenderer) th.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Custom renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                if (!sel) {
                    c.setBackground(row == hoverRow ? BG_CARD2 : (row % 2 == 0 ? BG_CARD : BG_APP));
                    c.setForeground(TEXT_PRI);
                    if (col == 1 && value != null) {
                        c.setForeground(typeColor(value.toString()));
                        ((JLabel) c).setFont(((JLabel) c).getFont().deriveFont(Font.BOLD));
                    }
                    if (col == 4 && value != null) {
                        boolean active = "Active".equals(value.toString());
                        c.setForeground(active ? EMERALD : TEXT_MUT);
                        ((JLabel) c).setFont(((JLabel) c).getFont().deriveFont(Font.BOLD));
                    }
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setPreferredSize(new Dimension(0, 280));
        tableCard.add(scroll);
        tableCard.add(Box.createVerticalStrut(10));

        // Delete selected button
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        DashboardUI.QuietButton deleteBtn = new DashboardUI.QuietButton("Delete Selected");
        deleteBtn.setForeground(DANGER);
        deleteBtn.addActionListener(e -> deleteSelected());
        DashboardUI.QuietButton deactivateAllBtn = new DashboardUI.QuietButton("Deactivate All");
        deactivateAllBtn.addActionListener(e -> deactivateAll());
        actionRow.add(deleteBtn);
        actionRow.add(Box.createHorizontalStrut(10));
        actionRow.add(deactivateAllBtn);
        tableCard.add(actionRow);

        page.add(tableCard);
        root.add(DashboardUI.scroll(page), BorderLayout.CENTER);
    }

    // ── Data operations ───────────────────────────────────────────────────────

    private void loadNotices() {
        tableModel.setRowCount(0);
        String sql = "SELECT notice_id, type, title, message, is_active, created_by, created_at " +
                     "FROM notices ORDER BY created_at DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("notice_id"),
                    rs.getString("type"),
                    rs.getString("title"),
                    rs.getString("message"),
                    rs.getBoolean("is_active") ? "Active" : "Inactive",
                    rs.getString("created_by"),
                    rs.getTimestamp("created_at") != null
                        ? rs.getTimestamp("created_at").toLocalDateTime()
                             .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                        : "—"
                });
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void postNotice() {
        String title   = titleField.getText().trim();
        String message = messageArea.getText().trim();
        String type    = (String) typeBox.getSelectedItem();

        if (title.isEmpty()) {
            ToastNotification.show(this, "Please enter a notice title.", ToastNotification.Type.WARNING);
            titleField.requestFocus();
            return;
        }
        if (message.isEmpty()) {
            ToastNotification.show(this, "Please enter a message body.", ToastNotification.Type.WARNING);
            messageArea.requestFocus();
            return;
        }

        String sql = "INSERT INTO notices (title, message, type, is_active, created_by) VALUES (?, ?, ?, 1, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.setString(4, adminName);
            ps.executeUpdate();
            AuditLog.log(adminName, "BROADCAST_NOTICE", "Posted notice: " + title);
            titleField.setText("");
            messageArea.setText("");
            loadNotices();
            ToastNotification.show(this, "Notice posted and is now live!", ToastNotification.Type.SUCCESS);
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to post notice: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleStatus(int row) {
        int id = (int) tableModel.getValueAt(row, 0);
        boolean currentlyActive = "Active".equals(tableModel.getValueAt(row, 4).toString());
        String sql = "UPDATE notices SET is_active = ? WHERE notice_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, !currentlyActive);
            ps.setInt(2, id);
            ps.executeUpdate();
            loadNotices();
            String state = currentlyActive ? "deactivated" : "activated";
            ToastNotification.show(this, "Notice " + state + ".", ToastNotification.Type.SUCCESS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastNotification.show(this, "Select a notice row to delete.", ToastNotification.Type.WARNING);
            return;
        }
        String title = tableModel.getValueAt(row, 2).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete notice \"" + title + "\"? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM notices WHERE notice_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
            AuditLog.log(adminName, "DELETE_NOTICE", "Deleted notice: " + title);
            loadNotices();
            ToastNotification.show(this, "Notice deleted.", ToastNotification.Type.SUCCESS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deactivateAll() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Deactivate all active notices? Students will no longer see any banners.",
                "Confirm Deactivate All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("UPDATE notices SET is_active = 0");
            loadNotices();
            ToastNotification.show(this, "All notices deactivated.", ToastNotification.Type.SUCCESS);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    static Color typeColor(String type) {
        if (type == null) return DashboardUI.TEXT_MUT;
        switch (type) {
            case TYPE_URGENT:  return DashboardUI.DANGER;
            case TYPE_WARNING: return DashboardUI.AMBER;
            default:           return DashboardUI.ACCENT;
        }
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_MUT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // ── Static helper used by StudentDashboardFrame ───────────────────────────

    /**
     * Returns all currently active notices (newest first). Returns empty list
     * if the table doesn't exist yet or there are no active notices.
     */
    public static java.util.List<Notice> fetchActiveNotices() {
        java.util.List<Notice> list = new java.util.ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT notice_id, type, title, message FROM notices " +
                 "WHERE is_active = 1 ORDER BY created_at DESC LIMIT 5");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Notice n = new Notice();
                n.id      = rs.getInt("notice_id");
                n.type    = rs.getString("type");
                n.title   = rs.getString("title");
                n.message = rs.getString("message");
                list.add(n);
            }
        } catch (SQLException ex) {
            // Table may not exist yet — silent fail
        }
        return list;
    }

    public static final class Notice {
        public int    id;
        public String type, title, message;
    }
}
