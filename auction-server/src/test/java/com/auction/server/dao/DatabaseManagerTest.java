package com.auction.server.dao;

import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho DatabaseManager — Singleton + SQLite connection.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseManagerTest {

    @BeforeAll
    static void setup() {
        DatabaseManager.resetInstance();
    }

    @Test
    @Order(1)
    @DisplayName("Singleton — getInstance() trả về cùng 1 instance")
    void testSingleton() {
        DatabaseManager db1 = DatabaseManager.getInstance();
        DatabaseManager db2 = DatabaseManager.getInstance();
        assertSame(db1, db2, "getInstance() phải trả về cùng 1 object");
    }

    @Test
    @Order(2)
    @DisplayName("Connection không null và đang mở")
    void testConnectionNotNull() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        assertNotNull(conn);
        assertDoesNotThrow(() -> assertFalse(conn.isClosed()));
    }

    @Test
    @Order(3)
    @DisplayName("Gọi getConnection() nhiều lần — cùng 1 connection")
    void testSameConnection() {
        Connection conn1 = DatabaseManager.getInstance().getConnection();
        Connection conn2 = DatabaseManager.getInstance().getConnection();
        assertSame(conn1, conn2);
    }

    @Test
    @Order(4)
    @DisplayName("Schema đã được tạo — bảng users tồn tại")
    void testSchemaCreated() {
        assertDoesNotThrow(() -> {
            Connection conn = DatabaseManager.getInstance().getConnection();
            var rs = conn.createStatement().executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='users'");
            assertTrue(rs.next(), "Bảng users phải tồn tại");
        });
    }

    @Test
    @Order(5)
    @DisplayName("Schema — bảng auctions tồn tại")
    void testAuctionsTableExists() {
        assertDoesNotThrow(() -> {
            Connection conn = DatabaseManager.getInstance().getConnection();
            var rs = conn.createStatement().executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='auctions'");
            assertTrue(rs.next(), "Bảng auctions phải tồn tại");
        });
    }

    @Test
    @Order(6)
    @DisplayName("Admin mặc định đã được tạo")
    void testDefaultAdmin() {
        assertDoesNotThrow(() -> {
            Connection conn = DatabaseManager.getInstance().getConnection();
            var rs = conn.createStatement().executeQuery(
                    "SELECT * FROM users WHERE username='admin'");
            assertTrue(rs.next(), "Admin mặc định phải tồn tại");
            assertEquals("ADMIN", rs.getString("role"));
        });
    }

    @Test
    @Order(7)
    @DisplayName("Reset instance — tạo lại kết nối mới")
    void testResetInstance() {
        Connection oldConn = DatabaseManager.getInstance().getConnection();
        DatabaseManager.resetInstance();
        Connection newConn = DatabaseManager.getInstance().getConnection();
        assertNotSame(oldConn, newConn);
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.getInstance().close();
        java.io.File dbFile = new java.io.File("auction.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
}
