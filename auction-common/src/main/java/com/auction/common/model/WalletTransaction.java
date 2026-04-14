package com.auction.common.model;

import java.time.LocalDateTime;

/**
 * WalletTransaction — lịch sử giao dịch ví tiền.
 * Ghi lại mọi thay đổi số dư: nạp, rút, cọc, hoàn cọc, thanh toán...
 */
public class WalletTransaction extends Entity {

    private static final long serialVersionUID = 1L;

    private int userId;
    private WalletTransactionType type;
    private double amount;
    private String description;

    public WalletTransaction() {
        super();
    }

    public WalletTransaction(int userId, WalletTransactionType type,
                             double amount, String description) {
        super();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.description = description;
    }

    public WalletTransaction(int id, int userId, WalletTransactionType type,
                             double amount, String description, LocalDateTime createdAt) {
        super(id);
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.description = description;
        setCreatedAt(createdAt);
    }

    // --- Getter / Setter ---

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public void setType(WalletTransactionType type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String printInfo() {
        return String.format("[Wallet] User: %d, Type: %s, Amount: %.2f - %s",
                userId, type, amount, description);
    }
}
