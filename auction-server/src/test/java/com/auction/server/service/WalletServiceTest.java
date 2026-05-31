package com.auction.server.service;

import com.auction.common.exception.InsufficientBalanceException;
import com.auction.common.exception.NotFoundException;
import com.auction.common.model.*;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho WalletService — logic ví tiền.
 * Test: nạp tiền, rút tiền, kiểm tra số dư, lịch sử.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WalletServiceTest {

    private static WalletService walletService;
    private static UserDAO userDAO;
    private static int userId;

    @BeforeAll
    static void setup() throws Exception {
        DatabaseManager.resetInstance();
        DatabaseManager.getInstance();
        walletService = new WalletService();
        userDAO = new UserDAO();

        Bidder user = new Bidder("wallet_test_user", "pass123", "Wallet Tester", "wallet@test.com");
        user = (Bidder) userDAO.insert(user);
        userId = user.getId();
    }

    @Test
    @Order(1)
    @DisplayName("Số dư ban đầu = 0")
    void testInitialBalance() throws Exception {
        double balance = walletService.getBalance(userId);
        assertEquals(0.0, balance);
    }

    @Test
    @Order(2)
    @DisplayName("Nạp tiền thành công")
    void testTopUp() throws Exception {
        double newBalance = walletService.topUp(userId, 500000);
        assertEquals(500000, newBalance);
    }

    @Test
    @Order(3)
    @DisplayName("Nạp tiền lần 2 — cộng dồn")
    void testTopUpAgain() throws Exception {
        double newBalance = walletService.topUp(userId, 200000);
        assertEquals(700000, newBalance);
    }

    @Test
    @Order(4)
    @DisplayName("Nạp số tiền âm → exception")
    void testTopUpNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                walletService.topUp(userId, -100));
    }

    @Test
    @Order(5)
    @DisplayName("Nạp số tiền = 0 → exception")
    void testTopUpZero() {
        assertThrows(IllegalArgumentException.class, () ->
                walletService.topUp(userId, 0));
    }

    @Test
    @Order(6)
    @DisplayName("Rút tiền thành công")
    void testWithdraw() throws Exception {
        double newBalance = walletService.withdraw(userId, 100000);
        assertEquals(600000, newBalance);
    }

    @Test
    @Order(7)
    @DisplayName("Rút nhiều hơn số dư → exception")
    void testWithdrawInsufficient() {
        assertThrows(InsufficientBalanceException.class, () ->
                walletService.withdraw(userId, 999999));
    }

    @Test
    @Order(8)
    @DisplayName("Tạm giữ tiền cọc thành công")
    void testHoldDeposit() throws Exception {
        walletService.holdDeposit(userId, 50000, 1);
        double balance = walletService.getBalance(userId);
        assertEquals(550000, balance);
    }

    @Test
    @Order(9)
    @DisplayName("Hoàn tiền cọc thành công")
    void testRefundDeposit() throws Exception {
        walletService.refundDeposit(userId, 50000, 1);
        double balance = walletService.getBalance(userId);
        assertEquals(600000, balance);
    }

    @Test
    @Order(10)
    @DisplayName("Lịch sử giao dịch đúng số lượng")
    void testWalletHistory() throws Exception {
        List<WalletTransaction> history = walletService.getHistory(userId);
        // topUp x2, withdraw x1, hold x1, refund x1 = 5
        assertEquals(5, history.size());
    }

    @Test
    @Order(11)
    @DisplayName("User không tồn tại → exception")
    void testNonExistentUser() {
        assertThrows(NotFoundException.class, () ->
                walletService.getBalance(99999));
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
