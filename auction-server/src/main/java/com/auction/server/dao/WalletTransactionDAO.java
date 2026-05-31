package com.auction.server.dao;

import com.auction.common.model.WalletTransaction;
import com.auction.common.model.WalletTransactionType;
import com.auction.common.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * WalletTransactionDAO — Data Access Object cho WalletTransaction.
 * Ghi lại lịch sử mọi thay đổi số dư ví tiền của user.
 *
 * THREAD-SAFETY: Mọi method bọc trong synchronized(DatabaseManager.getDbLock()).
 */
public class WalletTransactionDAO {

    private static final Logger logger = LoggerFactory.getLogger(WalletTransactionDAO.class);

    private Connection getConnection() {
        return DatabaseManager.getInstance().getConnection();
    }

    private WalletTransaction mapRow(ResultSet rs) throws SQLException {
        return new WalletTransaction(
            rs.getInt("id"),
            rs.getInt("user_id"),
            WalletTransactionType.valueOf(rs.getString("type")),
            rs.getDouble("amount"),
            rs.getString("description"),
            DateTimeUtil.parseDb(rs.getString("created_at"))
        );
    }

    public WalletTransaction insert(WalletTransaction tx) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "INSERT INTO wallet_transactions (user_id, type, amount, description) " +
                         "VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, tx.getUserId());
                ps.setString(2, tx.getType().name());
                ps.setDouble(3, tx.getAmount());
                ps.setString(4, tx.getDescription());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        tx.setId(keys.getInt(1));
                    }
                }
            }
            logger.info("Wallet tx: user={}, type={}, amount={}",
                    tx.getUserId(), tx.getType(), tx.getAmount());
            return tx;
        }
    }

    public List<WalletTransaction> findByUserId(int userId) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC";
            List<WalletTransaction> transactions = new ArrayList<>();

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        transactions.add(mapRow(rs));
                    }
                }
            }
            return transactions;
        }
    }

    public List<WalletTransaction> findByUserIdAndType(int userId, WalletTransactionType type) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM wallet_transactions WHERE user_id = ? AND type = ? " +
                         "ORDER BY created_at DESC";
            List<WalletTransaction> transactions = new ArrayList<>();

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setString(2, type.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        transactions.add(mapRow(rs));
                    }
                }
            }
            return transactions;
        }
    }
}
