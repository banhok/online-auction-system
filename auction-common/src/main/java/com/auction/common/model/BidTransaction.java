package com.auction.common.model;

import java.time.LocalDateTime;

/**
 * BidTransaction — giao dịch đặt giá.
 * Mỗi lần bidder đặt giá tạo ra 1 BidTransaction.
 * Lưu lại lịch sử toàn bộ bid trong 1 phiên đấu giá.
 */
public class BidTransaction extends Entity {

    private static final long serialVersionUID = 1L;

    private int auctionId;
    private int bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;
    private boolean isValid;

    public BidTransaction() {
        super();
        this.bidTime = LocalDateTime.now();
        this.isValid = true;
    }

    public BidTransaction(int auctionId, int bidderId, double bidAmount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
        this.isValid = true;
    }

    public BidTransaction(int id, int auctionId, int bidderId,
                          double bidAmount, LocalDateTime bidTime, boolean isValid) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isValid = isValid;
    }

    // --- Getter / Setter ---

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    @Override
    public String printInfo() {
        return String.format("[Bid] Auction: %d, Bidder: %d, Amount: %.2f, Time: %s",
                auctionId, bidderId, bidAmount, bidTime);
    }
}
