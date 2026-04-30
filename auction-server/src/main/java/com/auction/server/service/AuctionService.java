package com.auction.server.service;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Response;
import com.auction.common.exception.InsufficientBalanceException;
import com.auction.common.exception.InvalidBidException;
import com.auction.common.exception.NotFoundException;
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
    private final WalletService walletService;
    private final AuctionEventManager eventManager;
    private final EndStrategy endStrategy;

    // Lock riêng cho từng phiên đấu giá — tránh lost update khi bid đồng thời
    private final ConcurrentHashMap<Integer, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionService(AuctionEventManager eventManager) {
        this.eventManager = eventManager;
        this.walletService = new WalletService(eventManager);
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

        Auction auction = new Auction(itemId, sellerId, startingPrice, startTime, endTime);
        auction = auctionDAO.insert(auction);

        logger.info("Created auction #{} for item {}", auction.getId(), itemId);
        return auction;
    }

    /**
     * Tham gia phiên đấu giá (đặt cọc).
     * Lock per-auction để chặn TOCTOU giữa hasDeposit() và insert():
     * không có lock, 2 request JOIN cùng userId/auctionId có thể cùng pass check
     * → walletService.holdDeposit chạy 2 lần → trừ tiền đôi → insert thứ 2 fail UNIQUE
     * (kết quả: balance bị trừ thừa mà không có deposit row tương ứng).
     */
    public void joinAuction(int userId, int auctionId) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new NotFoundException("Phiên đấu giá không tồn tại"));

            if (auction.getStatus() != AuctionStatus.RUNNING &&
                auction.getStatus() != AuctionStatus.OPEN) {
                throw new InvalidBidException("Phiên đấu giá không ở trạng thái có thể tham gia");
            }

            if (userId == auction.getSellerId()) {
                throw new InvalidBidException("Seller không thể tham gia đấu giá sản phẩm của mình");
            }

            if (depositDAO.hasDeposit(userId, auctionId)) {
                throw new InvalidBidException("Bạn đã đặt cọc cho phiên này rồi");
            }

            double depositAmount = auction.getDepositAmount();
            walletService.holdDeposit(userId, depositAmount, auctionId);

            Deposit deposit = new Deposit(userId, auctionId, depositAmount);
            depositDAO.insert(deposit);

            logger.info("User {} joined auction #{} with deposit {}", userId, auctionId, depositAmount);
        } finally {
            lock.unlock();
        }
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
                    .orElseThrow(() -> new NotFoundException("Phiên đấu giá không tồn tại"));

            // 2. Kiểm tra trạng thái phiên
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new InvalidBidException("Phiên đấu giá không đang diễn ra");
            }

            // 3. Kiểm tra phiên đã hết giờ chưa
            if (auction.isExpired()) {
                throw new InvalidBidException("Phiên đấu giá đã hết thời gian");
            }

            // 4. Kiểm tra user đã đặt cọc chưa
            if (!depositDAO.hasDeposit(bidderId, auctionId)) {
                throw new InvalidBidException("Bạn chưa đặt cọc cho phiên này");
            }

            // 5. Kiểm tra seller không tự bid
            if (bidderId == auction.getSellerId()) {
                throw new InvalidBidException("Seller không thể đặt giá cho sản phẩm của mình");
            }

            // 6. Kiểm tra giá đặt phải cao hơn giá hiện tại
            if (bidAmount <= auction.getCurrentPrice()) {
                throw new InvalidBidException(
                        String.format("Giá đặt phải cao hơn giá hiện tại (%.2f)", auction.getCurrentPrice()));
            }

            // 7. Kiểm tra balance đủ không (giá đặt không vượt quá balance + cọc)
            User bidder = userDAO.findById(bidderId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại"));
            Deposit deposit = depositDAO.findByUserAndAuction(bidderId, auctionId)
                    .orElseThrow(() -> new NotFoundException("Deposit không tồn tại"));
            if (bidAmount > bidder.getBalance() + deposit.getAmount()) {
                throw new InsufficientBalanceException("Số dư không đủ để đặt giá này");
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
        data.put("bidTimeDb", DateTimeUtil.formatDb(bid.getBidTime())); // raw để client parse cho chart
        data.put("endTime", DateTimeUtil.formatDb(auction.getEndTime()));
        // Tên người đặt — client hiển thị thay vì "User #X"
        try {
            userDAO.findById(bid.getBidderId()).ifPresent(u ->
                    data.put("bidderName",
                            u.getFullName() != null && !u.getFullName().isEmpty()
                                    ? u.getFullName() : u.getUsername()));
        } catch (Exception ignored) { }
        try {
            data.put("participantCount", depositDAO.countHeldByAuction(auction.getId()));
        } catch (Exception e) {
            data.put("participantCount", 0);
        }

        // Thông báo tới tất cả subscriber của phiên này
        Response bidUpdate = new Response(CommandType.BID_UPDATE, true, "Có bid mới", data);
        eventManager.notifyAuction(auction.getId(), bidUpdate);

        // Nếu gia hạn → thông báo riêng
        if (extended) {
            Response extendNotify = new Response(CommandType.AUCTION_EXTENDED, true,
                    "Phiên đấu giá được gia hạn", data);
            eventManager.notifyAuction(auction.getId(), extendNotify);
        }
    }

    /**
     * Kết thúc phiên đấu giá.
     */
    public void finishAuction(int auctionId) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new NotFoundException("Phiên không tồn tại"));

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
            // Tên winner — client hiển thị thay "User #X"
            if (auction.getWinnerId() != null) {
                try {
                    userDAO.findById(auction.getWinnerId()).ifPresent(u ->
                            data.put("winnerName",
                                    u.getFullName() != null && !u.getFullName().isEmpty()
                                            ? u.getFullName() : u.getUsername()));
                } catch (Exception ignored) { }
            }
            // Lúc này tất cả deposit đã chuyển REFUNDED/DEDUCTED → count = 0; vẫn gửi để UI sync về 0
            try {
                data.put("participantCount", depositDAO.countHeldByAuction(auctionId));
            } catch (Exception e) {
                data.put("participantCount", 0);
            }

            Response endNotify = new Response(CommandType.AUCTION_ENDED, true,
                    "Phiên đấu giá đã kết thúc", data);
            eventManager.notifyAuction(auctionId, endNotify);
            // Broadcast tới toàn bộ client (kể cả user đang ở dashboard, không subscribe phiên này)
            // để danh sách phiên + tab "Phiên đã thắng" tự refresh ngay khi có phiên vừa FINISHED.
            eventManager.broadcast(endNotify);

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
     * Winner thanh toán sau khi thắng phiên (FINISHED → PAID).
     * Validate: auction ở trạng thái FINISHED, caller đúng là winner, deposit đã DEDUCTED.
     * Flow: processPayment cho winner → creditSeller → update status PAID.
     */
    public void payForWon(int callerId, int auctionId) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new NotFoundException("Phiên đấu giá không tồn tại"));

            if (auction.getStatus() != AuctionStatus.FINISHED) {
                throw new InvalidBidException("Chỉ có thể thanh toán phiên đã kết thúc");
            }
            if (auction.getWinnerId() == null || !auction.getWinnerId().equals(callerId)) {
                throw new InvalidBidException("Bạn không phải người thắng phiên này");
            }

            Deposit deposit = depositDAO.findByUserAndAuction(callerId, auctionId)
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy cọc của bạn cho phiên này"));

            double finalPrice = auction.getCurrentPrice();
            double depositHeld = deposit.getAmount();

            // Winner trả phần còn lại (tổng − cọc đã giữ)
            walletService.processPayment(callerId, finalPrice, depositHeld, auctionId);

            // Seller nhận đúng finalPrice
            walletService.creditSeller(auction.getSellerId(), finalPrice, auctionId);

            auction.setStatus(AuctionStatus.PAID);
            auctionDAO.update(auction);

            logger.info("Auction #{} paid by winner {}. Seller {} received {}",
                    auctionId, callerId, auction.getSellerId(), finalPrice);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Seller huỷ phiên — chỉ cho phép khi status OPEN hoặc RUNNING.
     * Refund toàn bộ deposit HELD về ví bidder, set status = CANCELED, broadcast cho UI sync.
     */
    public void cancelAuction(int callerId, int auctionId) throws Exception {
        ReentrantLock lock = getLock(auctionId);
        lock.lock();
        try {
            Auction auction = auctionDAO.findById(auctionId)
                    .orElseThrow(() -> new NotFoundException("Phiên đấu giá không tồn tại"));

            if (auction.getSellerId() != callerId) {
                throw new com.auction.common.exception.AuthenticationException(
                        "Bạn không có quyền huỷ phiên đấu giá này");
            }
            if (auction.getStatus() != AuctionStatus.OPEN
                    && auction.getStatus() != AuctionStatus.RUNNING) {
                throw new InvalidBidException("Chỉ huỷ được phiên ở trạng thái OPEN hoặc RUNNING");
            }

            // Refund toàn bộ HELD deposit về ví bidder
            List<Deposit> deposits = depositDAO.findHeldByAuction(auctionId);
            for (Deposit deposit : deposits) {
                depositDAO.updateStatus(deposit.getId(), DepositStatus.REFUNDED);
                walletService.refundDeposit(deposit.getUserId(), deposit.getAmount(), auctionId);

                Response refundNotify = Response.ok(CommandType.DEPOSIT_REFUNDED,
                        String.format("Phiên #%d đã bị huỷ — hoàn cọc %.2f",
                                auctionId, deposit.getAmount()));
                eventManager.notifyUser(deposit.getUserId(), refundNotify);
            }

            auction.setStatus(AuctionStatus.CANCELED);
            auctionDAO.update(auction);

            // Bắn event cho subscriber + dashboard refresh.
            // Tận dụng AUCTION_ENDED (UI đã handle status FINISHED/CANCELED) — winnerId null.
            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", auctionId);
            data.put("finalPrice", auction.getCurrentPrice());
            data.put("winnerId", null);
            data.put("bidCount", auction.getBidCount());
            data.put("participantCount", 0);
            data.put("canceled", true);
            Response endNotify = new Response(CommandType.AUCTION_ENDED, true,
                    "Phiên đã bị huỷ", data);
            eventManager.notifyAuction(auctionId, endNotify);
            eventManager.broadcast(endNotify);

            eventManager.clearAuction(auctionId);
            auctionLocks.remove(auctionId);

            logger.info("Auction #{} CANCELED by seller {}, refunded {} deposits",
                    auctionId, callerId, deposits.size());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Lấy phiên đấu giá theo id.
     */
    public Auction getAuction(int auctionId) throws Exception {
        return auctionDAO.findById(auctionId)
                .orElseThrow(() -> new NotFoundException("Phiên đấu giá không tồn tại"));
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
     * Lấy các phiên mà user đã thắng (FINISHED hoặc PAID).
     */
    public List<Auction> getMyWins(int winnerId) throws Exception {
        return auctionDAO.findByWinnerId(winnerId);
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
