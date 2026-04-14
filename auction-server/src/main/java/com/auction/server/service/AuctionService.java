package com.auction.server.service;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Response;
import com.auction.common.model.*;
import com.auction.common.util.DateTimeUtil;
import com.auction.server.dao.*;
import com.auction.server.observer.AuctionEventManager;
import com.auction.server.strategy.AntiSnipeEndStrategy;
import com.auction.server.strategy.EndStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AuctionService — xử lý logic đấu giá.
 * Đây là trái tim của hệ thống:
 * - Tạo/quản lý phiên đấu giá
 * - Xử lý đặt giá (concurrent-safe với ReentrantLock)
 * - Anti-sniping (Strategy pattern)
 * - Thông báo realtime (Observer pattern)
 * - Đặt cọc/hoàn cọc
 */
public class AuctionService {

    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidTransactionDAO bidTxDAO = new BidTransactionDAO();
    private final DepositDAO depositDAO = new DepositDAO();
    private final UserDAO userDAO = new UserDAO();
    private final WalletService walletService = new WalletService();
    private final AuctionEventManager eventManager;
    private final EndStrategy endStrategy;

    // Lock riêng cho từng phiên đấu giá — tránh lost update khi bid đồng thời
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionService(AuctionEventManager eventManager) {
        this.eventManager = eventManager;
        this.endStrategy = new AntiSnipeEndStrategy(); // Mặc định dùng anti-snipe
    }

    /**
     * Lấy lock cho 1 phiên đấu giá (tạo mới nếu chưa có).
     */
    private ReentrantLock getLock(int auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock(true)); // fair lock
    }

    /**
     * Tạo phiên đấu giá mới.
     */
    public Auction createAuction(int itemId, int sellerId, double startingPrice,
                                  LocalDateTime startTime, LocalDateTime endTime) throws Exception {

        if (startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }

        // Seller đặt cọc
        double sellerDeposit = startingPrice * 0.1;
        walletService.holdDeposit(sellerId, sellerDeposit, -1); // -1 tạm, sẽ update sau

        Auction auction = new Auction(itemId, sellerId, startingPrice, startTime, endTime);
        auction = auctionDAO.insert(auction);

        logger.info("Created auction #{} for item {}", auction.getId(), itemId);
        return auction;
    }

    /**
     * Tham gia phiên đấu giá (đặt cọc).
     */
    public void joinAuction(int userId, int auctionId) throws Exception {
        Auction auction = getAuction(auctionId);

        if (auction.getStatus() != AuctionStatus.RUNNING &&
            auction.getStatus() != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Phiên đấu giá không ở trạng thái có thể tham gia");
        }

        if (userId == auction.getSellerId()) {
            throw new IllegalArgumentException("Seller không thể tham gia đấu giá sản phẩm của mình");
        }

        // Kiểm tra đã đặt cọc chưa
        if (depositDAO.hasDeposit(userId, auctionId)) {
            throw new IllegalArgumentException("Bạn đã đặt cọc cho phiên này rồi");
        }

        // Đặt cọc
        double depositAmount = auction.getDepositAmount();
        walletService.holdDeposit(userId, depositAmount, auctionId);

        Deposit deposit = new Deposit(userId, auctionId, depositAmount);
        depositDAO.insert(deposit);

        logger.info("User {} joined auction #{} with deposit {}", userId, auctionId, depositAmount);
    }

    /**
     * ĐẶT GIÁ — Concurrent-safe.
     * Đây là method quan trọng nhất, dùng ReentrantLock để đảm bảo
     * chỉ 1 bid được xử lý tại 1 thời điểm cho mỗi phiên.
     */
    public BidTransaction placeBid(int auctionId, int bidderId, double bidAmount) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock(); // Khóa phiên — chỉ 1 thread xử lý tại 1 thời điểm
        try {
            // 1. Lấy auction mới nhất từ DB (tránh stale data)
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new IllegalArgumentException("Phiên đấu giá không tồn tại"));

            // 2. Kiểm tra trạng thái phiên
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new IllegalArgumentException("Phiên đấu giá không đang diễn ra");
            }

            // 3. Kiểm tra phiên đã hết giờ chưa
            if (auction.isExpired()) {
                throw new IllegalArgumentException("Phiên đấu giá đã hết thời gian");
            }

            // 4. Kiểm tra user đã đặt cọc chưa
            if (!depositDAO.hasDeposit(bidderId, auctionId)) {
                throw new IllegalArgumentException("Bạn chưa đặt cọc cho phiên này");
            }

            // 5. Kiểm tra seller không tự bid
            if (bidderId == auction.getSellerId()) {
                throw new IllegalArgumentException("Seller không thể đặt giá cho sản phẩm của mình");
            }

            // 6. Kiểm tra giá đặt phải cao hơn giá hiện tại
            if (bidAmount <= auction.getCurrentPrice()) {
                throw new IllegalArgumentException(
                        String.format("Giá đặt phải cao hơn giá hiện tại (%.2f)", auction.getCurrentPrice()));
            }

            // 7. Kiểm tra balance đủ không (giá đặt không vượt quá balance + cọc)
            User bidder = userDAO.findById(bidderId)
                    .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
            Deposit deposit = depositDAO.findByUserAndAuction(bidderId, auctionId)
                    .orElseThrow(() -> new IllegalArgumentException("Deposit không tồn tại"));
            if (bidAmount > bidder.getBalance() + deposit.getAmount()) {
                throw new IllegalArgumentException("Số dư không đủ để đặt giá này");
            }

            // 8. Tạo bid transaction
            BidTransaction bidTx = new BidTransaction(auctionId, bidderId, bidAmount);
            bidTx = bidTxDAO.insert(bidTx);

            // 9. Cập nhật auction
            auction.applyBid(bidAmount);
            auction.setWinnerId(bidderId);

            // 10. Anti-sniping — Strategy pattern
            boolean extended = endStrategy.handleNewBid(auction);

            // 11. Lưu auction vào DB
            auctionDAO.update(auction);

            // 12. Thông báo realtime — Observer pattern
            notifyBidUpdate(auction, bidTx, extended);

            logger.info("Bid placed: auction={}, bidder={}, amount={}, extended={}",
                    auctionId, bidderId, bidAmount, extended);
            return bidTx;

        } finally {
            lock.unlock(); // Luôn unlock dù có exception
        }
    }

    /**
     * Gửi thông báo bid mới tới tất cả subscriber.
     */
    private void notifyBidUpdate(Auction auction, BidTransaction bid, boolean extended) {
        Map<String, Object> data = new HashMap<>();
        data.put("auctionId", auction.getId());
        data.put("currentPrice", auction.getCurrentPrice());
        data.put("bidCount", auction.getBidCount());
        data.put("bidderId", bid.getBidderId());
        data.put("bidAmount", bid.getBidAmount());
        data.put("bidTime", DateTimeUtil.formatDisplay(bid.getBidTime()));
        data.put("endTime", DateTimeUtil.formatDb(auction.getEndTime()));

        // Thông báo tới tất cả subscriber của phiên này
        Response bidUpdate = new Response(CommandType.BID_UPDATE, true, "Có bid mới", data);
        eventManager.notifyAuction(auction.getId(), bidUpdate);

        // Nếu gia hạn → thông báo riêng
        if (extended) {
            Response extendNotify = new Response(CommandType.AUCTION_EXTENDED, true,
                    "Phiên đấu giá được gia hạn", data);
            eventManager.notifyAuction(auction.getId(), extendNotify);
        }

        // Thông báo outbid cho người đặt giá cao nhất trước đó
        // (nếu không phải chính người vừa bid)
    }

    /**
     * Kết thúc phiên đấu giá.
     */
    public void finishAuction(int auctionId) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new IllegalArgumentException("Phiên không tồn tại"));

            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return; // Đã xử lý rồi
            }

            auction.setStatus(AuctionStatus.FINISHED);
            auctionDAO.update(auction);

            // Xử lý cọc
            List<Deposit> deposits = depositDAO.findHeldByAuction(auctionId);
            for (Deposit deposit : deposits) {
                if (auction.getWinnerId() != null && deposit.getUserId() == auction.getWinnerId()) {
                    // Người thắng: trừ cọc vào thanh toán
                    depositDAO.updateStatus(deposit.getId(), DepositStatus.DEDUCTED);
                } else {
                    // Người thua: hoàn cọc
                    depositDAO.updateStatus(deposit.getId(), DepositStatus.REFUNDED);
                    walletService.refundDeposit(deposit.getUserId(), deposit.getAmount(), auctionId);

                    // Thông báo hoàn cọc
                    Response refundNotify = Response.ok(CommandType.DEPOSIT_REFUNDED,
                            String.format("Hoàn cọc %.2f cho phiên #%d", deposit.getAmount(), auctionId));
                    eventManager.notifyUser(deposit.getUserId(), refundNotify);
                }
            }

            // Thông báo phiên kết thúc
            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", auctionId);
            data.put("finalPrice", auction.getCurrentPrice());
            data.put("winnerId", auction.getWinnerId());
            data.put("bidCount", auction.getBidCount());

            Response endNotify = new Response(CommandType.AUCTION_ENDED, true,
                    "Phiên đấu giá đã kết thúc", data);
            eventManager.notifyAuction(auctionId, endNotify);

            // Cleanup
            eventManager.clearAuction(auctionId);
            auctionLocks.remove(auctionId);

            logger.info("Auction #{} finished. Winner: {}, Price: {}",
                    auctionId, auction.getWinnerId(), auction.getCurrentPrice());

        } finally {
            lock.unlock();
        }
    }

    /**
     * Lấy phiên đấu giá theo id.
     */
    public Auction getAuction(int auctionId) throws Exception {
        return auctionDAO.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Phiên đấu giá không tồn tại"));
    }

    /**
     * Lấy tất cả phiên.
     */
    public List<Auction> getAllAuctions() throws Exception {
        return auctionDAO.findAll();
    }

    /**
     * Lọc theo trạng thái.
     */
    public List<Auction> getAuctionsByStatus(String statusStr) throws Exception {
        AuctionStatus status = AuctionStatus.valueOf(statusStr.toUpperCase());
        return auctionDAO.findByStatus(status);
    }

    /**
     * Lấy theo seller.
     */
    public List<Auction> getAuctionsBySeller(int sellerId) throws Exception {
        return auctionDAO.findBySellerId(sellerId);
    }

    /**
     * Lấy lịch sử bid của 1 phiên.
     */
    public List<BidTransaction> getBidHistory(int auctionId) throws Exception {
        return bidTxDAO.findByAuctionId(auctionId);
    }

    /**
     * Lấy các phiên đã hết giờ nhưng chưa đóng (cho scheduler).
     */
    public List<Auction> getExpiredRunning() throws Exception {
        return auctionDAO.findExpiredRunning();
    }
}
