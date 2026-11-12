import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

/**
 * Manage Users Screen — dark themed.
 * Allows Admin to create, view, search, and delete Student and Faculty accounts.
 */
public class ManageUsersFrame extends JFrame {

    // ── Dark Palette ──────────────────────────────────────────────────────────
    private static final Color BG_APP   = new Color(15,  17,  23);
    private static final Color BG_CARD  = new Color(26,  29,  46);
    private static final Color BG_CARD2 = new Color(37,  40,  64);
    private static final Color ACCENT   = new Color(77,  142, 247);
    private static final Color TEXT_PRI = new Color(226, 232, 240);
    private static final Color TEXT_MUT = new Color(148, 163, 184);
    private static final Color BORDER   = new Color(45,  49,  84);
    private static final Color DANGER   = new Color(239, 68,  68);

    private final String adminName;

    // Creation form components
    private JTextField    fullNameField;
    private JTextField    usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;
    private JButton       createUserBtn;

    // Table & filter components
    private DefaultTableModel tableModel;
    private JTable            userTable;
    private JTextField        searchField;
    private JComboBox<String> filterRoleCombo;
    private JButton           deleteUserBtn;

    public ManageUsersFrame(String adminName) {
        super("AssessPro – Manage Users");
        this.adminName = adminName;
        UIUtils.setWindowIcon(this);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        buildUI();
        loadUsers();
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
            new EmptyBorder(14, 30, 14, 30)
        ));

        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(ACCENT);
        JLabel adminLabel = new JLabel("USER MANAGEMENT");
        adminLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        adminLabel.setForeground(TEXT_MUT);
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logo);
        branding.add(adminLabel);

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
            new AdminDashboardFrame(adminName).setVisible(true);
        });

        header.add(branding, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel();
        content.setBackground(BG_APP);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(25, 35, 35, 35));

        JLabel heading = new JLabel("User Management");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(TEXT_PRI);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Create new student and faculty accounts or remove existing users.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(heading);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(20));

        // Create User Card
        JPanel createCard = new JPanel();
        createCard.setBackground(BG_CARD);
        createCard.setLayout(new BoxLayout(createCard, BoxLayout.Y_AXIS));
        createCard.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(18, 22, 18, 22)));
        createCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        createCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));

        JLabel createTitle = new JLabel("Create New Account");
        createTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        createTitle.setForeground(TEXT_PRI);
        createTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        createCard.add(createTitle);
        createCard.add(Box.createVerticalStrut(12));

        JPanel formGrid = new JPanel(new GridLayout(2, 4, 14, 6));
        formGrid.setOpaque(false);
        formGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        formGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        fullNameField  = createStyledTextField();
        usernameField  = createStyledTextField();
        passwordField  = new JPasswordField();
        UIUtils.styleTextField(passwordField, 8, 12, 8, 12);

        roleCombo = new JComboBox<>(new String[]{"Student", "Faculty"});
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleCombo.setBackground(BG_CARD2);
        roleCombo.setForeground(TEXT_PRI);

        formGrid.add(fieldLabel("FULL NAME"));
        formGrid.add(fieldLabel("USERNAME"));
        formGrid.add(fieldLabel("PASSWORD"));
        formGrid.add(fieldLabel("ROLE"));

        formGrid.add(fullNameField);
        formGrid.add(usernameField);
        formGrid.add(passwordField);
        formGrid.add(roleCombo);

        createCard.add(formGrid);
        createCard.add(Box.createVerticalStrut(12));

        createUserBtn = new JButton("Add User Account");
        createUserBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        createUserBtn.setBackground(ACCENT);
        createUserBtn.setForeground(Color.WHITE);
        createUserBtn.setFocusPainted(false);
        createUserBtn.setBorderPainted(false);
        createUserBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        createUserBtn.setBorder(new EmptyBorder(8, 18, 8, 18));
        createUserBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createUserBtn.addActionListener(e -> createUser());
        createCard.add(createUserBtn);

        content.add(createCard);
        content.add(Box.createVerticalStrut(22));

        // Table Card
        JPanel tableCard = new JPanel();
        tableCard.setBackground(BG_CARD);
        tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS));
        tableCard.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(18, 22, 18, 22)));
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));

        // Search & Filter Toolbar
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTools.setOpaque(false);

        searchField = new JTextField(18);
        UIUtils.styleTextField(searchField, 7, 10, 7, 10);
        searchField.addActionListener(e -> loadUsers());

        JButton searchBtn = new JButton("Search");
        styleActionButton(searchBtn, ACCENT);
        searchBtn.addActionListener(e -> loadUsers());

        filterRoleCombo = new JComboBox<>(new String[]{"All Roles", "Student", "Faculty", "Admin"});
        filterRoleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        filterRoleCombo.setBackground(BG_CARD2);
        filterRoleCombo.setForeground(TEXT_PRI);
        filterRoleCombo.addActionListener(e -> loadUsers());

        JButton refreshBtn = new JButton("Refresh");
        styleActionButton(refreshBtn, BG_CARD2);
        refreshBtn.setForeground(TEXT_PRI);
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            filterRoleCombo.setSelectedIndex(0);
            loadUsers();
        });

        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setForeground(TEXT_MUT);
        JLabel roleLbl = new JLabel("Role:");
        roleLbl.setForeground(TEXT_MUT);

        leftTools.add(searchLbl);
        leftTools.add(searchField);
        leftTools.add(searchBtn);
        leftTools.add(roleLbl);
        leftTools.add(filterRoleCombo);
        leftTools.add(refreshBtn);

        deleteUserBtn = new JButton("Delete Selected User");
        deleteUserBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteUserBtn.setBackground(DANGER);
        deleteUserBtn.setForeground(Color.WHITE);
        deleteUserBtn.setFocusPainted(false);
        deleteUserBtn.setBorderPainted(false);
        deleteUserBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteUserBtn.setBorder(new EmptyBorder(8, 16, 8, 16));
        deleteUserBtn.addActionListener(e -> deleteSelectedUser());

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(deleteUserBtn, BorderLayout.EAST);

        tableCard.add(toolbar);
        tableCard.add(Box.createVerticalStrut(14));

        // JTable
        String[] columns = {"User ID", "Username", "Full Name", "Role", "Created At"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        userTable = new JTable(tableModel);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userTable.setForeground(TEXT_PRI);
        userTable.setBackground(BG_CARD);
        userTable.setRowHeight(32);
        userTable.setShowVerticalLines(false);
        userTable.setGridColor(BORDER);
        userTable.setSelectionBackground(BG_CARD2);
        userTable.setSelectionForeground(TEXT_PRI);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader th = userTable.getTableHeader();
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
                    c.setBackground(row % 2 == 0 ? BG_CARD : BG_APP);
                    c.setForeground(TEXT_PRI);
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        };
        for (int i = 0; i < userTable.getColumnCount(); i++) {
            userTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(userTable);
        tableScroll.setBorder(new LineBorder(BORDER, 1, true));
        tableScroll.getViewport().setBackground(BG_CARD);
        tableScroll.setBackground(BG_CARD);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(0, 280));

        tableCard.add(tableScroll);
        content.add(tableCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setBackground(BG_APP);
        scroll.getViewport().setBackground(BG_APP);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);
    }

    private JTextField createStyledTextField() {
        JTextField f = new JTextField();
        UIUtils.styleTextField(f, 8, 12, 8, 12);
        return f;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(TEXT_MUT);
        return l;
    }

    private void styleActionButton(JButton btn, Color bg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
    }

    private void createUser() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String role     = (String) roleCombo.getSelectedItem();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.", "Missing Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.length() < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters.", "Weak Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, role);
            ps.executeUpdate();
            AuditLog.log(adminName, "CREATE_USER", role + " account '" + username + "' (" + fullName + ")");

            JOptionPane.showMessageDialog(this, role + " account created successfully for " + fullName + "!", "AssessPro", JOptionPane.INFORMATION_MESSAGE);
            fullNameField.setText("");
            usernameField.setText("");
            passwordField.setText("");
            loadUsers();
        } catch (SQLException ex) {
            if ("23000".equals(ex.getSQLState())) {
                JOptionPane.showMessageDialog(this, "Username already exists. Please choose a different username.", "Duplicate Username", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Could not create user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadUsers() {
        tableModel.setRowCount(0);
        String search       = searchField.getText().trim();
        String selectedRole = (String) filterRoleCombo.getSelectedItem();

        StringBuilder sql = new StringBuilder("SELECT user_id, username, full_name, role, created_at FROM users WHERE 1=1 ");

        if (!search.isEmpty()) {
            sql.append("AND (username LIKE ? OR full_name LIKE ?) ");
        }
        if (selectedRole != null && !"All Roles".equals(selectedRole)) {
            sql.append("AND role = ? ");
        }
        sql.append("ORDER BY user_id DESC");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (!search.isEmpty()) {
                String pattern = "%" + search + "%";
                ps.setString(paramIndex++, pattern);
                ps.setString(paramIndex++, pattern);
            }
            if (selectedRole != null && !"All Roles".equals(selectedRole)) {
                ps.setString(paramIndex++, selectedRole);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at")
                    });
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.", "No User Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int    userId   = (Integer) tableModel.getValueAt(selectedRow, 0);
        String username = (String)  tableModel.getValueAt(selectedRow, 1);
        String role     = (String)  tableModel.getValueAt(selectedRow, 3);

        if (adminName.equalsIgnoreCase(username)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own logged-in admin account!", "Action Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the user '" + username + "' (" + role + ")?\nThis will remove their account and related records.",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) return;

        // Remove the user's exam answers and attempts first, then the account itself,
        // all in one transaction so a failure never leaves half-deleted data.
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement p1 = con.prepareStatement(
                     "DELETE FROM student_answers WHERE attempt_id IN (SELECT attempt_id FROM exam_attempts WHERE student_id = ?)");
                 PreparedStatement p2 = con.prepareStatement("DELETE FROM exam_attempts WHERE student_id = ?");
                 PreparedStatement p3 = con.prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                p1.setInt(1, userId); p1.executeUpdate();
                p2.setInt(1, userId); p2.executeUpdate();
                p3.setInt(1, userId); p3.executeUpdate();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
            AuditLog.log(adminName, "DELETE_USER", role + " account '" + username + "'");
            JOptionPane.showMessageDialog(this, "User '" + username + "' deleted successfully.", "AssessPro", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not delete user: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
