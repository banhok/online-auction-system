package com.auction.server.dao;

import com.auction.common.model.Deposit;
import com.auction.common.model.DepositStatus;
import com.auction.common.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DepositDAO — Data Access Object cho Deposit (đặt cọc).
 * Quản lý tiền cọc khi user tham gia phiên đấu giá.
 */
public class DepositDAO {

    private static final Logger logger = LoggerFactory.getLogger(DepositDAO.class);

    private Connection getConnection() {
        return DatabaseManager.getInstance().getConnection();
    }

    private Deposit mapRow(ResultSet rs) throws SQLException {
        Deposit deposit = new Deposit(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getInt("auction_id"),
            rs.getDouble("amount"),
            DepositStatus.valueOf(rs.getString("status"))
        );
        deposit.setCreatedAt(DateTimeUtil.parseDb(rs.getString("created_at")));
        deposit.setUpdatedAt(DateTimeUtil.parseDb(rs.getString("updated_at")));
        return deposit;
    }

    /**
     * Tạo deposit mới.
     */
    public Deposit insert(Deposit deposit) throws SQLException {
        String sql = "INSERT INTO deposits (user_id, auction_id, amount, status) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, deposit.getUserId());
            ps.setInt(2, deposit.getAuctionId());
            ps.setDouble(3, deposit.getAmount());
            ps.setString(4, deposit.getStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    deposit.setId(keys.getInt(1));
                }
            }
        }
        logger.info("Inserted deposit: user={}, auction={}, amount={}",
                deposit.getUserId(), deposit.getAuctionId(), deposit.getAmount());
        return deposit;
    }

    /**
     * Tìm deposit theo user và auction.
     */
    public Optional<Deposit> findByUserAndAuction(int userId, int auctionId) throws SQLException {
        String sql = "SELECT * FROM deposits WHERE user_id = ? AND auction_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy tất cả deposit đang HELD của 1 auction.
     */
    public List<Deposit> findHeldByAuction(int auctionId) throws SQLException {
        String sql = "SELECT * FROM deposits WHERE auction_id = ? AND status = 'HELD'";
        List<Deposit> deposits = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deposits.add(mapRow(rs));
                }
            }
        }
        return deposits;
    }

    /**
     * Lấy tất cả deposit của 1 user.
     */
    public List<Deposit> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM deposits WHERE user_id = ? ORDER BY created_at DESC";
        List<Deposit> deposits = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    deposits.add(mapRow(rs));
                }
            }
        }
        return deposits;
    }

    /**
     * Cập nhật trạng thái deposit.
     */
    public void updateStatus(int depositId, DepositStatus status) throws SQLException {
        String sql = "UPDATE deposits SET status = ?, updated_at = datetime('now') WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, depositId);
            ps.executeUpdate();
        }
        logger.info("Updated deposit #{}: status={}", depositId, status);
    }

    /**
     * Cập nhật trạng thái deposit theo user và auction.
     */
    public void updateStatusByUserAndAuction(int userId, int auctionId, DepositStatus status) throws SQLException {
        String sql = "UPDATE deposits SET status = ?, updated_at = datetime('now') " +
                     "WHERE user_id = ? AND auction_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            ps.setInt(3, auctionId);
            ps.executeUpdate();
        }
    }

    /**
     * Kiểm tra user đã đặt cọc cho auction chưa.
     */
    public boolean hasDeposit(int userId, int auctionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM deposits WHERE user_id = ? AND auction_id = ? AND status = 'HELD'";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
