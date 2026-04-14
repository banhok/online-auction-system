package com.auction.common.model;

import java.time.LocalDateTime;

/**
 * Auction — phiên đấu giá.
 * Quản lý trạng thái, thời gian, giá hiện tại, anti-sniping.
 * Trạng thái: OPEN -> RUNNING -> FINISHED -> PAID / CANCELED
 */
public class Auction extends Entity {

    private static final long serialVersionUID = 1L;

    private int itemId;
    private int sellerId;
    private double startingPrice;
    private double currentPrice;
    private int bidCount;
    private AuctionStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;
    private Integer winnerId;           // null nếu chưa có người thắng
    private double depositAmount;       // Tiền cọc yêu cầu (10% giá khởi điểm)
    private int antiSnipeSeconds;       // Bid trong X giây cuối thì gia hạn
    private int antiSnipeExtension;     // Gia hạn thêm Y giây

    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.bidCount = 0;
        this.antiSnipeSeconds = 60;
        this.antiSnipeExtension = 60;
    }

    public Auction(int itemId, int sellerId, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.bidCount = 0;
        this.status = AuctionStatus.OPEN;
        this.startTime = startTime;
        this.endTime = endTime;
        this.originalEndTime = endTime;
        this.depositAmount = startingPrice * 0.1; // 10% giá khởi điểm
        this.antiSnipeSeconds = 60;
        this.antiSnipeExtension = 60;
    }

    // --- Getter / Setter ---

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getBidCount() {
        return bidCount;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getOriginalEndTime() {
        return originalEndTime;
    }

    public void setOriginalEndTime(LocalDateTime originalEndTime) {
        this.originalEndTime = originalEndTime;
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
    }

    public double getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(double depositAmount) {
        this.depositAmount = depositAmount;
    }

    public int getAntiSnipeSeconds() {
        return antiSnipeSeconds;
    }

    public void setAntiSnipeSeconds(int antiSnipeSeconds) {
        this.antiSnipeSeconds = antiSnipeSeconds;
    }

    public int getAntiSnipeExtension() {
        return antiSnipeExtension;
    }

    public void setAntiSnipeExtension(int antiSnipeExtension) {
        this.antiSnipeExtension = antiSnipeExtension;
    }

    // --- Business logic helpers ---

    /**
     * Kiểm tra phiên đấu giá có đang chạy không.
     */
    public boolean isRunning() {
        return status == AuctionStatus.RUNNING;
    }

    /**
     * Kiểm tra phiên đã kết thúc chưa (theo thời gian).
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endTime);
    }

    /**
     * Kiểm tra bid có nằm trong vùng anti-snipe không.
     * Trả về true nếu thời gian còn lại <= antiSnipeSeconds.
     */
    public boolean isInAntiSnipeZone() {
        LocalDateTime snipeThreshold = endTime.minusSeconds(antiSnipeSeconds);
        return LocalDateTime.now().isAfter(snipeThreshold);
    }

    /**
     * Gia hạn thêm thời gian (anti-sniping).
     */
    public void extendEndTime() {
        this.endTime = this.endTime.plusSeconds(antiSnipeExtension);
    }

    /**
     * Tăng bid count và cập nhật giá.
     */
    public void applyBid(double newPrice) {
        this.currentPrice = newPrice;
        this.bidCount++;
        this.setUpdatedAt(LocalDateTime.now());
    }

    @Override
    public String printInfo() {
        return String.format("[Auction #%d] Item: %d, Price: %.2f, Status: %s, Bids: %d",
                getId(), itemId, currentPrice, status, bidCount);
    }
}
