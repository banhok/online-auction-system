package com.auction.common.model;

/**
 * Admin — quản trị viên hệ thống.
 * Kế thừa User, role = ADMIN.
 * Có thể: quản lý user, xem tổng quan hệ thống.
 */
public class Admin extends User {

    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
        setRole(UserRole.ADMIN);
    }

    public Admin(int id, String username, String password, String fullName,
                 String email, double balance) {
        super(id, username, password, fullName, email, UserRole.ADMIN, balance);
    }

    public Admin(String username, String password, String fullName, String email) {
        super(username, password, fullName, email, UserRole.ADMIN);
    }

    @Override
    public String printInfo() {
        return String.format("[ADMIN] %s (%s)", getFullName(), getUsername());
    }
}
