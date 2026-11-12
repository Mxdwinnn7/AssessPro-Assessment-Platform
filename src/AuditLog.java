import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Records admin actions (who did what, and when) in the audit_log table.
 *
 * The table is created automatically the first time it is needed, so no manual
 * database setup is required. Logging never throws: a problem writing the audit
 * trail must not stop the action the admin was actually performing.
 */
public final class AuditLog {

    private static boolean tableReady = false;

    private AuditLog() {}

    /** Creates the audit_log table if it does not exist yet. Safe to call repeatedly. */
    public static synchronized void ensureTable() {
        if (tableReady) return;
        String ddl = "CREATE TABLE IF NOT EXISTS audit_log (" +
                     "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                     "actor VARCHAR(100) NOT NULL, " +
                     "action VARCHAR(40) NOT NULL, " +
                     "details VARCHAR(500), " +
                     "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                     "INDEX idx_audit_created (created_at))";
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {
            st.execute(ddl);
            tableReady = true;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /** Adds one entry to the audit trail. Never throws. */
    public static void log(String actor, String action, String details) {
        ensureTable();
        String safeDetails = details == null ? "" : details;
        if (safeDetails.length() > 500) safeDetails = safeDetails.substring(0, 497) + "...";
        String sql = "INSERT INTO audit_log (actor, action, details) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, actor == null || actor.isBlank() ? "unknown" : actor);
            ps.setString(2, action);
            ps.setString(3, safeDetails);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    /** Shortens long text (e.g. a question) for use in a log line. */
    public static String clip(String text, int max) {
        if (text == null) return "";
        String t = text.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
