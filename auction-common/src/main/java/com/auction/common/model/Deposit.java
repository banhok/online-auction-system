package com.auction.common.model;

/**
 * Deposit — tiền đặt cọc khi tham gia đấu giá.
 * Trạng thái: HELD -> REFUNDED (thua) / DEDUCTED (thắng)
 */
public class Deposit extends Entity {

    private static final long serialVersionUID = 1L;

    private int userId;
    private int auctionId;
    private double amount;
    private DepositStatus status;

    public Deposit() {
        super();
        this.status = DepositStatus.HELD;
    }

    public Deposit(int userId, int auctionId, double amount) {
        super();
        this.userId = userId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.status = DepositStatus.HELD;
    }

    public Deposit(int id, int userId, int auctionId, double amount, DepositStatus status) {
        super(id);
        this.userId = userId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.status = status;
    }

    // --- Getter / Setter ---

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public void setStatus(DepositStatus status) {
        this.status = status;
    }

    @Override
    public String printInfo() {
        return String.format("[Deposit] User: %d, Auction: %d, Amount: %.2f, Status: %s",
                userId, auctionId, amount, status);
    }
}
