package com.auction.common.model;

/**
 * Lớp trừu tượng User — kế thừa Entity.
 * Chứa thông tin chung của mọi người dùng.
 * Các lớp con: Bidder, Seller, Admin.
 * Inheritance: Entity -> User -> Bidder/Seller/Admin
 */
public abstract class User extends Entity {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String fullName;
    private String email;
    private UserRole role;
    private double balance;

    // Constructor mặc định
    protected User() {
        super();
    }

    // Constructor đầy đủ
    protected User(int id, String username, String password, String fullName,
                   String email, UserRole role, double balance) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    // Constructor không có id (dùng khi tạo mới, id do DB sinh)
    protected User(String username, String password, String fullName,
                   String email, UserRole role) {
        super();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.balance = 0.0;
    }

    // --- Getter / Setter ---

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * Kiểm tra số dư có đủ không.
     */
    public boolean hasEnoughBalance(double amount) {
        return this.balance >= amount;
    }

    @Override
    public String printInfo() {
        return String.format("[%s] %s (%s) - Balance: %.2f",
                role, fullName, username, balance);
    }
}
