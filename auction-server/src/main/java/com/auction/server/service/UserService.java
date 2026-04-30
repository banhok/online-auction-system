package com.auction.server.service;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.exception.NotFoundException;
import com.auction.common.model.*;
import com.auction.server.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * UserService — xử lý business logic liên quan đến User.
 * Đăng nhập, đăng ký, cập nhật profile, quản lý user (admin).
 */
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO = new UserDAO();

    /**
     * Đăng ký tài khoản mới.
     */
    public User register(String username, String password, String fullName,
                         String email, String roleStr) throws Exception {

        // Validate input
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        // Kiểm tra trùng
        if (userDAO.existsByUsername(username)) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }
        if (userDAO.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // Tạo user theo role
        UserRole role = UserRole.valueOf(roleStr.toUpperCase());
        User user;
        switch (role) {
            case BIDDER:
                user = new Bidder(username, password, fullName, email);
                break;
            case SELLER:
                user = new Seller(username, password, fullName, email);
                break;
            default:
                throw new IllegalArgumentException("Không thể đăng ký với role: " + role);
        }

        return userDAO.insert(user);
    }

    /**
     * Đăng nhập.
     */
    public User login(String username, String password) throws Exception {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username và mật khẩu không được để trống");
        }

        Optional<User> optUser = userDAO.findByUsername(username);
        if (optUser.isEmpty()) {
            throw new AuthenticationException("Tài khoản không tồn tại");
        }

        User user = optUser.get();
        if (!user.getPassword().equals(password)) {
            throw new AuthenticationException("Mật khẩu không chính xác");
        }

        logger.info("User logged in: {} ({})", username, user.getRole());
        return user;
    }

    /**
     * Lấy thông tin profile.
     */
    public User getProfile(int userId) throws Exception {
        return userDAO.findById(userId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại"));
    }

    /**
     * Cập nhật profile.
     */
    public User updateProfile(int userId, String fullName, String email) throws Exception {
        User user = getProfile(userId);

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        if (email != null && !email.trim().isEmpty()) {
            // Kiểm tra email trùng (trừ chính user đó)
            Optional<User> existing = userDAO.findByEmail(email);
            if (existing.isPresent() && existing.get().getId() != userId) {
                throw new IllegalArgumentException("Email đã được sử dụng bởi người khác");
            }
            user.setEmail(email);
        }

        userDAO.update(user);
        return user;
    }

    /**
     * Đổi mật khẩu.
     */
    public void changePassword(int userId, String oldPassword, String newPassword) throws Exception {
        User user = getProfile(userId);

        if (!user.getPassword().equals(oldPassword)) {
            throw new AuthenticationException("Mật khẩu cũ không chính xác");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
        }

        user.setPassword(newPassword);
        userDAO.update(user);
    }

    /**
     * Lấy tất cả user (Admin).
     */
    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    /**
     * Xóa user (Admin).
     */
    public void deleteUser(int userId) throws SQLException {
        userDAO.delete(userId);
    }
}
