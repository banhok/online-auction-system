package com.auction.common.model;

/**
 * Bidder — người tham gia đấu giá.
 * Kế thừa User, role = BIDDER.
 * Có thể: đặt giá, xem lịch sử, quản lý ví tiền.
 */
public class Bidder extends User {

    private static final long serialVersionUID = 1L;

    private int totalBids;
    private int auctionsWon;

    public Bidder() {
        super();
        setRole(UserRole.BIDDER);
    }

    public Bidder(int id, String username, String password, String fullName,
                  String email, double balance) {
        super(id, username, password, fullName, email, UserRole.BIDDER, balance);
        this.totalBids = 0;
        this.auctionsWon = 0;
    }

    public Bidder(String username, String password, String fullName, String email) {
        super(username, password, fullName, email, UserRole.BIDDER);
        this.totalBids = 0;
        this.auctionsWon = 0;
    }

    public int getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }

    public int getAuctionsWon() {
        return auctionsWon;
    }

    public void setAuctionsWon(int auctionsWon) {
        this.auctionsWon = auctionsWon;
    }

    @Override
    public String printInfo() {
        return String.format("[BIDDER] %s - Bids: %d, Won: %d, Balance: %.2f",
                getFullName(), totalBids, auctionsWon, getBalance());
    }
}
