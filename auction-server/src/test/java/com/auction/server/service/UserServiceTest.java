package com.auction.server.service;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.model.*;
import com.auction.server.dao.DatabaseManager;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho UserService — đăng ký, đăng nhập, quản lý user.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private static UserService userService;
    private static int testUserId;

    @BeforeAll
    static void setup() {
        DatabaseManager.resetInstance();
        DatabaseManager.getInstance();
        userService = new UserService();
    }

    @Test
    @Order(1)
    @DisplayName("Đăng ký Bidder thành công")
    void testRegisterBidder() throws Exception {
        User user = userService.register("user_test1", "pass123",
                "Test User 1", "user1@test.com", "BIDDER");
        testUserId = user.getId();

        assertNotNull(user);
        assertTrue(user.getId() > 0);
        assertEquals("user_test1", user.getUsername());
        assertEquals(UserRole.BIDDER, user.getRole());
        assertInstanceOf(Bidder.class, user);
    }

    @Test
    @Order(2)
    @DisplayName("Đăng ký Seller thành công")
    void testRegisterSeller() throws Exception {
        User user = userService.register("seller_test1", "pass123",
                "Test Seller", "seller1@test.com", "SELLER");

        assertNotNull(user);
        assertEquals(UserRole.SELLER, user.getRole());
        assertInstanceOf(Seller.class, user);
    }

    @Test
    @Order(3)
    @DisplayName("Đăng ký username trùng → exception")
    void testRegisterDuplicateUsername() {
        assertThrows(IllegalArgumentException.class, () ->
                userService.register("user_test1", "pass123",
                        "Duplicate", "other@test.com", "BIDDER"));
    }

    @Test
    @Order(4)
    @DisplayName("Đăng ký email trùng → exception")
    void testRegisterDuplicateEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                userService.register("user_new", "pass123",
                        "New User", "user1@test.com", "BIDDER"));
    }

    @Test
    @Order(5)
    @DisplayName("Đăng ký mật khẩu quá ngắn → exception")
    void testRegisterShortPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                userService.register("user_short", "123",
                        "Short", "short@test.com", "BIDDER"));
    }

    @Test
    @Order(6)
    @DisplayName("Đăng ký với role ADMIN → exception")
    void testRegisterAdmin() {
        assertThrows(IllegalArgumentException.class, () ->
                userService.register("admin_test", "pass123",
                        "Admin Test", "admin_test@test.com", "ADMIN"));
    }

    @Test
    @Order(7)
    @DisplayName("Đăng nhập thành công")
    void testLoginSuccess() throws Exception {
        User user = userService.login("user_test1", "pass123");
        assertNotNull(user);
        assertEquals("user_test1", user.getUsername());
    }

    @Test
    @Order(8)
    @DisplayName("Đăng nhập sai mật khẩu → exception")
    void testLoginWrongPassword() {
        assertThrows(AuthenticationException.class, () ->
                userService.login("user_test1", "wrongpass"));
    }

    @Test
    @Order(9)
    @DisplayName("Đăng nhập username không tồn tại → exception")
    void testLoginNotExist() {
        assertThrows(AuthenticationException.class, () ->
                userService.login("nonexistent", "pass123"));
    }

    @Test
    @Order(10)
    @DisplayName("Lấy profile thành công")
    void testGetProfile() throws Exception {
        User user = userService.getProfile(testUserId);
        assertNotNull(user);
        assertEquals("user_test1", user.getUsername());
    }

    @Test
    @Order(11)
    @DisplayName("Cập nhật profile thành công")
    void testUpdateProfile() throws Exception {
        User user = userService.updateProfile(testUserId, "Updated Name", "updated@test.com");
        assertEquals("Updated Name", user.getFullName());
        assertEquals("updated@test.com", user.getEmail());
    }

    @Test
    @Order(12)
    @DisplayName("Đổi mật khẩu thành công")
    void testChangePassword() throws Exception {
        assertDoesNotThrow(() ->
                userService.changePassword(testUserId, "pass123", "newpass123"));

        // Đăng nhập bằng mật khẩu mới
        User user = userService.login("user_test1", "newpass123");
        assertNotNull(user);
    }

    @Test
    @Order(13)
    @DisplayName("Đổi mật khẩu sai mật khẩu cũ → exception")
    void testChangePasswordWrongOld() {
        assertThrows(AuthenticationException.class, () ->
                userService.changePassword(testUserId, "wrongold", "newpass"));
    }

    @Test
    @Order(14)
    @DisplayName("Lấy tất cả users (Admin)")
    void testGetAllUsers() throws Exception {
        List<User> users = userService.getAllUsers();
        // admin mặc định + 2 user vừa tạo
        assertTrue(users.size() >= 3);
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
