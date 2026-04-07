package com.auction.common.model;

/**
 * Seller — người bán / đăng sản phẩm đấu giá.
 * Kế thừa User, role = SELLER.
 * Có thể: đăng sản phẩm, quản lý phiên đấu giá, xem thống kê.
 */
public class Seller extends User {

    private static final long serialVersionUID = 1L;

    private int totalItems;
    private int totalAuctions;

    public Seller() {
        super();
        setRole(UserRole.SELLER);
    }

    public Seller(int id, String username, String password, String fullName,
                  String email, double balance) {
        super(id, username, password, fullName, email, UserRole.SELLER, balance);
        this.totalItems = 0;
        this.totalAuctions = 0;
    }

    public Seller(String username, String password, String fullName, String email) {
        super(username, password, fullName, email, UserRole.SELLER);
        this.totalItems = 0;
        this.totalAuctions = 0;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(int totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    @Override
    public String printInfo() {
        return String.format("[SELLER] %s - Items: %d, Auctions: %d, Balance: %.2f",
                getFullName(), totalItems, totalAuctions, getBalance());
    }
}
