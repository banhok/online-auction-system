package com.auction.server.dao;

import com.auction.common.model.Auction;
import com.auction.common.model.AuctionStatus;
import com.auction.common.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AuctionDAO — Data Access Object cho Auction.
 * Quản lý CRUD và truy vấn phiên đấu giá trong database.
 */
public class AuctionDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuctionDAO.class);

    private Connection getConnection() {
        return DatabaseManager.getInstance().getConnection();
    }

    /**
     * Map ResultSet thành Auction object.
     */
    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getInt("id"));
        auction.setItemId(rs.getInt("item_id"));
        auction.setSellerId(rs.getInt("seller_id"));
        auction.setStartingPrice(rs.getDouble("starting_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setBidCount(rs.getInt("bid_count"));
        auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
        auction.setStartTime(DateTimeUtil.parseDb(rs.getString("start_time")));
        auction.setEndTime(DateTimeUtil.parseDb(rs.getString("end_time")));
        auction.setOriginalEndTime(DateTimeUtil.parseDb(rs.getString("original_end_time")));
        auction.setDepositAmount(rs.getDouble("deposit_amount"));
        auction.setAntiSnipeSeconds(rs.getInt("anti_snipe_seconds"));
        auction.setAntiSnipeExtension(rs.getInt("anti_snipe_extension"));
        auction.setCreatedAt(DateTimeUtil.parseDb(rs.getString("created_at")));
        auction.setUpdatedAt(DateTimeUtil.parseDb(rs.getString("updated_at")));

        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            auction.setWinnerId(winnerId);
        }

        return auction;
    }

    /**
     * Tạo phiên đấu giá mới.
     */
    public Auction insert(Auction auction) throws SQLException {
        String sql = "INSERT INTO auctions (item_id, seller_id, starting_price, current_price, " +
                     "bid_count, status, start_time, end_time, original_end_time, deposit_amount, " +
                     "anti_snipe_seconds, anti_snipe_extension) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, auction.getItemId());
            ps.setInt(2, auction.getSellerId());
            ps.setDouble(3, auction.getStartingPrice());
            ps.setDouble(4, auction.getCurrentPrice());
            ps.setInt(5, auction.getBidCount());
            ps.setString(6, auction.getStatus().name());
            ps.setString(7, DateTimeUtil.formatDb(auction.getStartTime()));
            ps.setString(8, DateTimeUtil.formatDb(auction.getEndTime()));
            ps.setString(9, DateTimeUtil.formatDb(auction.getOriginalEndTime()));
            ps.setDouble(10, auction.getDepositAmount());
            ps.setInt(11, auction.getAntiSnipeSeconds());
            ps.setInt(12, auction.getAntiSnipeExtension());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    auction.setId(keys.getInt(1));
                }
            }
        }
        logger.info("Inserted auction #{} for item {}", auction.getId(), auction.getItemId());
        return auction;
    }

    /**
     * Tìm auction theo id.
     */
    public Optional<Auction> findById(int id) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAuction(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Lấy tất cả auction.
     */
    public List<Auction> findAll() throws SQLException {
        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";
        List<Auction> auctions = new ArrayList<>();

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
        }
        return auctions;
    }

    /**
     * Lọc auction theo trạng thái.
     */
    public List<Auction> findByStatus(AuctionStatus status) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE status = ? ORDER BY end_time ASC";
        List<Auction> auctions = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapRowToAuction(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Lấy auction theo seller.
     */
    public List<Auction> findBySellerId(int sellerId) throws SQLException {
        String sql = "SELECT * FROM auctions WHERE seller_id = ? ORDER BY created_at DESC";
        List<Auction> auctions = new ArrayList<>();

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    auctions.add(mapRowToAuction(rs));
                }
            }
        }
        return auctions;
    }

    /**
     * Lấy các auction đang RUNNING và đã hết giờ (cần đóng).
     */
    public List<Auction> findExpiredRunning() throws SQLException {
        String sql = "SELECT * FROM auctions WHERE status = 'RUNNING' AND end_time <= datetime('now')";
        List<Auction> auctions = new ArrayList<>();

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
        }
        return auctions;
    }

    /**
     * Cập nhật auction (giá, bid count, status, thời gian...).
     */
    public void update(Auction auction) throws SQLException {
        String sql = "UPDATE auctions SET current_price = ?, bid_count = ?, status = ?, " +
                     "end_time = ?, winner_id = ?, updated_at = datetime('now') WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setDouble(1, auction.getCurrentPrice());
            ps.setInt(2, auction.getBidCount());
            ps.setString(3, auction.getStatus().name());
            ps.setString(4, DateTimeUtil.formatDb(auction.getEndTime()));
            if (auction.getWinnerId() != null) {
                ps.setInt(5, auction.getWinnerId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setInt(6, auction.getId());
            ps.executeUpdate();
        }
        logger.info("Updated auction #{}: status={}, price={}",
                auction.getId(), auction.getStatus(), auction.getCurrentPrice());
    }

    /**
     * Cập nhật trạng thái auction.
     */
    public void updateStatus(int auctionId, AuctionStatus status) throws SQLException {
        String sql = "UPDATE auctions SET status = ?, updated_at = datetime('now') WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, auctionId);
            ps.executeUpdate();
        }
    }

    /**
     * Xóa auction.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
        logger.info("Deleted auction: {}", id);
    }
}
