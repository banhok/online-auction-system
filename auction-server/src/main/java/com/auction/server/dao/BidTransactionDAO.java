package com.auction.server.dao;

import com.auction.common.model.BidTransaction;
import com.auction.common.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BidTransactionDAO — Data Access Object cho BidTransaction.
 * Lưu và truy vấn lịch sử đặt giá trong phiên đấu giá.
 */
public class BidTransactionDAO {

    private static final Logger logger = LoggerFactory.getLogger(BidTransactionDAO.class);

    private Connection getConnection() {
        return DatabaseManager.getInstance().getConnection();
    }

    private BidTransaction mapRow(ResultSet rs) throws SQLException {
        BidTransaction bid = new BidTransaction(
            rs.getInt("id"),
            rs.getInt("auction_id"),
            rs.getInt("bidder_id"),
            rs.getDouble("bid_amount"),
            DateTimeUtil.parseDb(rs.getString("bid_time")),
            rs.getInt("is_valid") == 1
        );
        return bid;
    }

    /**
     * Thêm bid mới.
     */
    public BidTransaction insert(BidTransaction bid) throws SQLException {
        String sql = "INSERT INTO bid_transactions (auction_id, bidder_id, bid_amount, bid_time, is_valid) " +
                     "VALUES (?, ?, ?, datetime('now'), ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, bid.getAuctionId());
            ps.setInt(2, bid.getBidderId());
            ps.setDouble(3, bid.getBidAmount());
            ps.setInt(4, bid.isValid() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    bid.setId(keys.getInt(1));
                }
            }
        }
        logger.info("Inserted bid: auction={}, bidder={}, amount={}",
                bid.getAuctionId(), bid.getBidderId(), bid.getBidAmount());
        return bid;
    }

    /**
     * Lấy lịch sử bid của 1 phiên đấu giá (sắp xếp theo thời gian).
     */
    public List<BidTransaction> findByAuctionId(int auctionId) throws SQLException {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? AND is_valid = 1 " +
                     "ORDER BY bid_time ASC";
        List<BidTransaction> bids = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapRow(rs));
                }
            }
        }
        return bids;
    }

    /**
     * Lấy bid cao nhất của 1 phiên đấu giá.
     */
    public Optional<BidTransaction> findHighestBid(int auctionId) throws SQLException {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? AND is_valid = 1 " +
                     "ORDER BY bid_amount DESC LIMIT 1";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy tất cả bid của 1 user.
     */
    public List<BidTransaction> findByBidderId(int bidderId) throws SQLException {
        String sql = "SELECT * FROM bid_transactions WHERE bidder_id = ? AND is_valid = 1 " +
                     "ORDER BY bid_time DESC";
        List<BidTransaction> bids = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, bidderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapRow(rs));
                }
            }
        }
        return bids;
    }

    /**
     * Đếm số bid trong 1 phiên.
     */
    public int countByAuctionId(int auctionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = ? AND is_valid = 1";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}
