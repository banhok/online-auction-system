package com.auction.server.dao;

import com.auction.common.model.*;
import com.auction.common.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * UserDAO — Data Access Object cho User.
 * THREAD-SAFETY: Mọi method bọc trong synchronized(DatabaseManager.getDbLock()).
 */
public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    private Connection getConnection() {
        return DatabaseManager.getInstance().getConnection();
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        UserRole role = UserRole.valueOf(rs.getString("role"));
        User user;

        switch (role) {
            case BIDDER:
                user = new Bidder(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getDouble("balance")
                );
                break;
            case SELLER:
                user = new Seller(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getDouble("balance")
                );
                break;
            case ADMIN:
                user = new Admin(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    rs.getDouble("balance")
                );
                break;
            default:
                throw new SQLException("Unknown role: " + role);
        }

        user.setCreatedAt(DateTimeUtil.parseDb(rs.getString("created_at")));
        user.setUpdatedAt(DateTimeUtil.parseDb(rs.getString("updated_at")));
        return user;
    }

    public User insert(User user) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "INSERT INTO users (username, password, full_name, email, role, balance) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getEmail());
                ps.setString(5, user.getRole().name());
                ps.setDouble(6, user.getBalance());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        user.setId(keys.getInt(1));
                    }
                }
            }
            logger.info("Inserted user: {} ({})", user.getUsername(), user.getRole());
            return user;
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM users WHERE username = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRowToUser(rs));
                    }
                }
            }
            return Optional.empty();
        }
    }

    public Optional<User> findById(int id) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM users WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRowToUser(rs));
                    }
                }
            }
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM users WHERE email = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRowToUser(rs));
                    }
                }
            }
            return Optional.empty();
        }
    }

    public List<User> findAll() throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM users ORDER BY created_at DESC";
            List<User> users = new ArrayList<>();

            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    users.add(mapRowToUser(rs));
                }
            }
            return users;
        }
    }

    public void update(User user) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "UPDATE users SET full_name = ?, email = ?, password = ?, " +
                         "balance = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPassword());
                ps.setDouble(4, user.getBalance());
                ps.setInt(5, user.getId());
                ps.executeUpdate();
            }
            logger.info("Updated user: {}", user.getUsername());
        }
    }

    public void updateBalance(int userId, double newBalance) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "UPDATE users SET balance = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setDouble(1, newBalance);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            logger.info("Updated balance for user {}: {}", userId, newBalance);
        }
    }

    public void delete(int id) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "DELETE FROM users WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            logger.info("Deleted user: {}", id);
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, email);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }
}
