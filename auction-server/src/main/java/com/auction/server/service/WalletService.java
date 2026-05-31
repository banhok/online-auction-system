package com.auction.server.service;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Response;
import com.auction.common.exception.InsufficientBalanceException;
import com.auction.common.exception.NotFoundException;
import com.auction.common.model.*;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.WalletTransactionDAO;
import com.auction.server.observer.AuctionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WalletService — xử lý logic ví tiền.
 * Nạp/rút tiền, tạm giữ cọc, hoàn cọc, thanh toán.
 * Tất cả thao tác đều được ghi lại trong wallet_transactions.
 * Sau mọi thay đổi balance đều push BALANCE_UPDATE event để UI client tự refresh.
 */
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final UserDAO userDAO = new UserDAO();
    private final WalletTransactionDAO walletTxDAO = new WalletTransactionDAO();
    private final AuctionEventManager eventManager;

    public WalletService() {
        this(null);
    }

    public WalletService(AuctionEventManager eventManager) {
        this.eventManager = eventManager;
    }

    /**
     * Push BALANCE_UPDATE event tới user — UI tự cập nhật label số dư.
     * No-op nếu eventManager null (test context).
     */
    private void notifyBalance(int userId, double newBalance) {
        if (eventManager == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("balance", newBalance);
        Response evt = new Response(CommandType.BALANCE_UPDATE, true, "Số dư cập nhật", data);
        eventManager.notifyUser(userId, evt);
    }

    /**
     * Lấy số dư hiện tại.
     */
    public double getBalance(int userId) throws Exception {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));
        return user.getBalance();
    }

    /**
     * Nạp tiền vào ví.
     */
    public double topUp(int userId, double amount) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }

        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        double newBalance = user.getBalance() + amount;
        userDAO.updateBalance(userId, newBalance);

        // Ghi lịch sử
        walletTxDAO.insert(new WalletTransaction(
                userId, WalletTransactionType.DEPOSIT, amount,
                String.format("Nạp tiền: +%.2f", amount)
        ));

        notifyBalance(userId, newBalance);
        logger.info("User {} topped up {}, new balance: {}", userId, amount, newBalance);
        return newBalance;
    }

    /**
     * Rút tiền từ ví.
     */
    public double withdraw(int userId, double amount) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        }

        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        if (user.getBalance() < amount) {
            throw new InsufficientBalanceException("Số dư không đủ");
        }

        double newBalance = user.getBalance() - amount;
        userDAO.updateBalance(userId, newBalance);

        walletTxDAO.insert(new WalletTransaction(
                userId, WalletTransactionType.WITHDRAW, amount,
                String.format("Rút tiền: -%.2f", amount)
        ));

        notifyBalance(userId, newBalance);
        return newBalance;
    }

    /**
     * Tạm giữ tiền cọc khi tham gia đấu giá.
     */
    public void holdDeposit(int userId, double amount, int auctionId) throws Exception {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        if (user.getBalance() < amount) {
            throw new InsufficientBalanceException(
                    String.format("Số dư không đủ để đặt cọc. Cần: %.2f, Có: %.2f",
                            amount, user.getBalance()));
        }

        double newBalance = user.getBalance() - amount;
        userDAO.updateBalance(userId, newBalance);

        walletTxDAO.insert(new WalletTransaction(
                userId, WalletTransactionType.BID_HOLD, amount,
                String.format("Đặt cọc phiên #%d: -%.2f", auctionId, amount)
        ));

        notifyBalance(userId, newBalance);
    }

    /**
     * Hoàn tiền cọc (khi thua đấu giá).
     */
    public void refundDeposit(int userId, double amount, int auctionId) throws Exception {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));

        double newBalance = user.getBalance() + amount;
        userDAO.updateBalance(userId, newBalance);

        walletTxDAO.insert(new WalletTransaction(
                userId, WalletTransactionType.BID_REFUND, amount,
                String.format("Hoàn cọc phiên #%d: +%.2f", auctionId, amount)
        ));

        notifyBalance(userId, newBalance);
    }

    /**
     * Thanh toán khi thắng đấu giá (giá cuối - tiền cọc đã giữ).
     */
    public void processPayment(int userId, double totalPrice, double depositHeld,
                                int auctionId) throws Exception {
        double remaining = totalPrice - depositHeld;

        if (remaining > 0) {
            User user = userDAO.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại"));

            if (user.getBalance() < remaining) {
                throw new InsufficientBalanceException("Số dư không đủ để thanh toán");
            }

            double newBalance = user.getBalance() - remaining;
            userDAO.updateBalance(userId, newBalance);
            notifyBalance(userId, newBalance);
        }

        walletTxDAO.insert(new WalletTransaction(
                userId, WalletTransactionType.PAYMENT, totalPrice,
                String.format("Thanh toán phiên #%d: -%.2f (cọc đã trừ: %.2f)",
                        auctionId, totalPrice, depositHeld)
        ));
    }

    /**
     * Cộng tiền vào ví seller sau khi winner thanh toán phiên.
     * Dùng type DEPOSIT để tránh phát sinh enum mới — description giải thích rõ nguồn.
     */
    public void creditSeller(int sellerId, double amount, int auctionId) throws Exception {
        User seller = userDAO.findById(sellerId)
                .orElseThrow(() -> new NotFoundException("Seller không tồn tại"));

        double newBalance = seller.getBalance() + amount;
        userDAO.updateBalance(sellerId, newBalance);

        walletTxDAO.insert(new WalletTransaction(
                sellerId, WalletTransactionType.DEPOSIT, amount,
                String.format("Nhận thanh toán phiên #%d: +%.2f", auctionId, amount)
        ));

        notifyBalance(sellerId, newBalance);
        logger.info("Seller {} received payment {} for auction {}, new balance: {}",
                sellerId, amount, auctionId, newBalance);
    }

    /**
     * Lấy lịch sử giao dịch ví.
     */
    public List<WalletTransaction> getHistory(int userId) throws Exception {
        return walletTxDAO.findByUserId(userId);
    }
}
