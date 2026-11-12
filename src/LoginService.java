import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {
    public String authenticate(String username, String password, String role)
            throws SQLException {
        String sql = """
            SELECT username
            FROM users
            WHERE username = ?
              AND password_hash = ?
              AND role = ?
            """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, role);

            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("username") : null;
            }
        }
    }
}
