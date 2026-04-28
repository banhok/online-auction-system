package com.auction.server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * DatabaseManager — Singleton pattern.
 * Quản lý kết nối SQLite duy nhất cho toàn bộ server.
 * Đảm bảo chỉ có 1 instance tồn tại trong toàn hệ thống.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    // Singleton instance — volatile đảm bảo thread-safe
    private static volatile DatabaseManager instance;

    private Connection connection;

    // Private constructor — không cho tạo từ bên ngoài
    private DatabaseManager() {
        try {
            connect();
            initializeDatabase();
            logger.info("DatabaseManager initialized successfully.");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Cannot initialize database", e);
        }
    }

    /**
     * Lấy instance duy nhất (Double-checked locking — thread-safe).
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Tạo kết nối tới SQLite.
     */
    private void connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            // Bật WAL mode để hỗ trợ đọc/ghi đồng thời tốt hơn
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA foreign_keys=ON");
            }
            logger.info("Connected to SQLite database.");
        }
    }

    /**
     * Khởi tạo database từ schema.sql.
     */
    private void initializeDatabase() throws SQLException {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql");
            if (is == null) {
                logger.warn("schema.sql not found in resources, skipping initialization.");
                return;
            }

            String schema;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                schema = reader.lines().collect(Collectors.joining("\n"));
            }

            // Tách các câu lệnh SQL bằng dấu ;
            schema = schema.replaceAll("--[^\n]*","");
            
            String[] statements = schema.split(";");
            try (Statement stmt = connection.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        stmt.execute(trimmed);
                    }
                }
            }
            logger.info("Database schema initialized.");
        } catch (Exception e) {
            logger.error("Failed to initialize database schema", e);
            throw new SQLException("Schema initialization failed", e);
        }
    }

    /**
     * Lấy connection hiện tại. Tự reconnect nếu bị đóng.
     *
     * CẢNH BÁO: java.sql.Connection KHÔNG thread-safe theo JDBC spec.
     * DAO PHẢI bọc mọi PreparedStatement.execute*() trong
     * {@code synchronized (DatabaseManager.getDbLock()) { ... }}
     * để chặn 2 thread cùng dùng 1 PreparedStatement / Connection.
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            logger.error("Failed to get connection", e);
            throw new RuntimeException("Database connection failed", e);
        }
        return connection;
    }

    /**
     * Lock object dùng chung cho mọi DAO khi thực thi SQL.
     * Vì server chia sẻ duy nhất 1 Connection (xem cảnh báo ở getConnection),
     * mọi DAO phải đồng bộ trên monitor này:
     *
     * <pre>
     *   synchronized (DatabaseManager.getDbLock()) {
     *       try (PreparedStatement ps = ...) { ps.executeUpdate(); }
     *   }
     * </pre>
     *
     * Ghi chú: per-auction ReentrantLock ở AuctionService chỉ serialize logic
     * trong cùng 1 phiên; 2 phiên khác nhau vẫn dùng chung Connection nên
     * cần thêm lock ở đây để tránh race ở JDBC layer.
     */
    public static Object getDbLock() {
        return DB_LOCK;
    }

    private static final Object DB_LOCK = new Object();

    /**
     * Đóng kết nối database.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.error("Failed to close database connection", e);
        }
    }

    /**
     * Reset instance (dùng cho Unit Test).
     */
    public static void resetInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
