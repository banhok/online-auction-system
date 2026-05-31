package com.auction.server.service;

import com.auction.common.exception.InvalidBidException;
import com.auction.common.model.*;
import com.auction.server.dao.*;
import com.auction.server.observer.AuctionEventManager;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho AuctionService — logic đấu giá.
 * Test các trường hợp: đặt giá hợp lệ, giá không hợp lệ,
 * phiên đã đóng, concurrent bid, anti-sniping.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuctionServiceTest {

    private static AuctionService auctionService;
    private static AuctionEventManager eventManager;
    private static UserDAO userDAO;
    private static ItemDAO itemDAO;
    private static AuctionDAO auctionDAO;
    private static DepositDAO depositDAO;

    private static int sellerId;
    private static int bidder1Id;
    private static int bidder2Id;
    private static int itemId;
    private static int auctionId;

    @BeforeAll
    static void setup() throws Exception {
        // Reset database cho test
        DatabaseManager.resetInstance();
        DatabaseManager.getInstance();

        eventManager = new AuctionEventManager();
        auctionService = new AuctionService(eventManager);
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        auctionDAO = new AuctionDAO();
        depositDAO = new DepositDAO();

        // Tạo test users
        Seller seller = new Seller("test_seller", "pass123", "Test Seller", "seller@test.com");
        seller.setBalance(1000000);
        seller = (Seller) userDAO.insert(seller);
        userDAO.updateBalance(seller.getId(), 1000000);
        sellerId = seller.getId();

        Bidder bidder1 = new Bidder("test_bidder1", "pass123", "Bidder One", "bidder1@test.com");
        bidder1.setBalance(500000);
        bidder1 = (Bidder) userDAO.insert(bidder1);
        userDAO.updateBalance(bidder1.getId(), 500000);
        bidder1Id = bidder1.getId();

        Bidder bidder2 = new Bidder("test_bidder2", "pass123", "Bidder Two", "bidder2@test.com");
        bidder2.setBalance(500000);
        bidder2 = (Bidder) userDAO.insert(bidder2);
        userDAO.updateBalance(bidder2.getId(), 500000);
        bidder2Id = bidder2.getId();

        // Tạo test item
        Electronics item = new Electronics(sellerId, "Test Phone", "A test phone",
                null, 100000, "Apple", "NEW", 12);
        item = (Electronics) itemDAO.insert(item);
        itemId = item.getId();
    }

    @Test
    @Order(1)
    @DisplayName("Tạo phiên đấu giá thành công")
    void testCreateAuction() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        Auction auction = auctionService.createAuction(itemId, sellerId, 100000, start, end);
        auctionId = auction.getId();

        assertNotNull(auction);
        assertTrue(auction.getId() > 0);
        assertEquals(100000, auction.getCurrentPrice());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(10000, auction.getDepositAmount()); // 10% of 100000
    }

    @Test
    @Order(2)
    @DisplayName("Tạo phiên với thời gian không hợp lệ → exception")
    void testCreateAuctionInvalidTime() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = LocalDateTime.now().plusHours(1);

        assertThrows(IllegalArgumentException.class, () ->
                auctionService.createAuction(itemId, sellerId, 100000, start, end));
    }

    @Test
    @Order(3)
    @DisplayName("Tham gia đấu giá (đặt cọc) thành công")
    void testJoinAuction() throws Exception {
        // Chuyển sang RUNNING để join
        auctionDAO.updateStatus(auctionId, AuctionStatus.RUNNING);

        auctionService.joinAuction(bidder1Id, auctionId);
        assertTrue(depositDAO.hasDeposit(bidder1Id, auctionId));
    }

    @Test
    @Order(4)
    @DisplayName("Seller không thể tham gia đấu giá sản phẩm của mình")
    void testSellerCannotJoin() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.joinAuction(sellerId, auctionId));
    }

    @Test
    @Order(5)
    @DisplayName("Đặt giá hợp lệ thành công")
    void testPlaceBidSuccess() throws Exception {
        BidTransaction bid = auctionService.placeBid(auctionId, bidder1Id, 150000);

        assertNotNull(bid);
        assertEquals(150000, bid.getBidAmount());
        assertEquals(bidder1Id, bid.getBidderId());

        Auction updated = auctionService.getAuction(auctionId);
        assertEquals(150000, updated.getCurrentPrice());
        assertEquals(1, updated.getBidCount());
        assertEquals(bidder1Id, updated.getWinnerId());
    }

    @Test
    @Order(6)
    @DisplayName("Đặt giá thấp hơn giá hiện tại → exception")
    void testPlaceBidTooLow() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid(auctionId, bidder1Id, 100000));
    }

    @Test
    @Order(7)
    @DisplayName("Đặt giá bằng giá hiện tại → exception")
    void testPlaceBidEqualCurrent() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid(auctionId, bidder1Id, 150000));
    }

    @Test
    @Order(8)
    @DisplayName("Đặt giá khi chưa đặt cọc → exception")
    void testPlaceBidWithoutDeposit() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid(auctionId, bidder2Id, 200000));
    }

    @Test
    @Order(9)
    @DisplayName("Bidder 2 tham gia và đặt giá cao hơn")
    void testSecondBidderOutbids() throws Exception {
        auctionService.joinAuction(bidder2Id, auctionId);
        BidTransaction bid = auctionService.placeBid(auctionId, bidder2Id, 200000);

        assertEquals(200000, bid.getBidAmount());
        Auction updated = auctionService.getAuction(auctionId);
        assertEquals(bidder2Id, updated.getWinnerId());
        assertEquals(2, updated.getBidCount());
    }

    @Test
    @Order(10)
    @DisplayName("Lấy lịch sử bid đúng số lượng")
    void testGetBidHistory() throws Exception {
        var bids = auctionService.getBidHistory(auctionId);
        assertEquals(2, bids.size());
        // Bid đầu tiên giá thấp hơn bid thứ hai
        assertTrue(bids.get(0).getBidAmount() < bids.get(1).getBidAmount());
    }

    @Test
    @Order(11)
    @DisplayName("Kết thúc phiên đấu giá — xác định winner đúng")
    void testFinishAuction() throws Exception {
        auctionService.finishAuction(auctionId);

        Auction finished = auctionService.getAuction(auctionId);
        assertEquals(AuctionStatus.FINISHED, finished.getStatus());
        assertEquals(bidder2Id, finished.getWinnerId());
        assertEquals(200000, finished.getCurrentPrice());
    }

    @Test
    @Order(12)
    @DisplayName("Đặt giá khi phiên đã kết thúc → exception")
    void testPlaceBidOnFinished() {
        assertThrows(InvalidBidException.class, () ->
                auctionService.placeBid(auctionId, bidder1Id, 300000));
    }

    /**
     * Concurrent bid test — chứng minh per-auction ReentrantLock chống lost update.
     *
     * 10 thread cùng gọi placeBid với 10 mức giá khác nhau (200k, 210k, ..., 290k).
     * Bid đến trong lock sớm sẽ thắng tạm thời; bid sau sẽ bị reject nếu giá thấp hơn
     * currentPrice tại thời điểm vào lock. Kết quả ĐÚNG: currentPrice cuối cùng phải là
     * mức cao nhất từng được commit (290k nếu thread 290k vào sau cùng, hoặc bất kỳ giá
     * cao hơn các giá đã commit). Bid count = số bid hợp lệ. Không có lost update —
     * nghĩa là số bid count >= 1 và currentPrice >= 200k và bằng giá cao nhất commit được.
     */
    @Test
    @Order(13)
    @DisplayName("N thread đặt giá đồng thời — không lost update, single winner")
    void testConcurrentBids() throws Exception {
        // Tạo phiên mới để không đụng auction đã FINISHED ở các test trên.
        // Item phải khác (item_id UNIQUE trong auctions).
        Electronics item = new Electronics(sellerId, "Concurrent Phone", "Test concurrent",
                null, 100000, "Apple", "NEW", 12);
        item = (Electronics) itemDAO.insert(item);

        Auction a = auctionService.createAuction(item.getId(), sellerId, 100000,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        int aId = a.getId();
        auctionDAO.updateStatus(aId, AuctionStatus.RUNNING);

        // Tạo 10 bidder, mỗi người join để có deposit.
        // Cần balance đủ ≥ 290k để bid cao nhất pass kiểm tra balance.
        int N = 10;
        int[] bidderIds = new int[N];
        for (int i = 0; i < N; i++) {
            Bidder b = new Bidder("conc_bidder_" + i, "p", "Concurrent " + i,
                    "conc" + i + "@t.com");
            b.setBalance(500000);
            b = (Bidder) userDAO.insert(b);
            userDAO.updateBalance(b.getId(), 500000);
            bidderIds[i] = b.getId();
            auctionService.joinAuction(b.getId(), aId);
        }

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(N);
        ExecutorService pool = Executors.newFixedThreadPool(N);
        AtomicInteger acceptedCount = new AtomicInteger(0);
        double[] bids = new double[N];
        for (int i = 0; i < N; i++) bids[i] = 200000 + i * 10000; // 200k → 290k

        for (int i = 0; i < N; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    start.await(); // đồng loạt fire
                    auctionService.placeBid(aId, bidderIds[idx], bids[idx]);
                    acceptedCount.incrementAndGet();
                } catch (InvalidBidException expected) {
                    // bid thấp hơn currentPrice tại lúc vào lock → bị reject (đúng mong đợi)
                } catch (Exception e) {
                    // lỗi không mong đợi
                    fail("Unexpected exception in concurrent bid: " + e.getMessage());
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Concurrent bids timed out");
        pool.shutdown();

        Auction finalAuction = auctionService.getAuction(aId);

        // Số bid hợp lệ = số thread thực sự pass (nhân được lock và bid > currentPrice tại lúc đó)
        assertEquals(acceptedCount.get(), finalAuction.getBidCount(),
                "Bid count phải khớp số bid được commit — nếu lệch là có lost update");

        // currentPrice cuối phải là 1 trong các giá đã đăng ký (200k..290k)
        // và ≥ 200k (ít nhất 1 thread thắng).
        assertTrue(finalAuction.getCurrentPrice() >= 200000,
                "currentPrice không thể thấp hơn bid hợp lệ thấp nhất");
        assertTrue(finalAuction.getCurrentPrice() <= 290000,
                "currentPrice không thể vượt quá bid cao nhất được đề xuất");

        // Có winner (single winner — winnerId là 1 trong bidderIds)
        assertNotNull(finalAuction.getWinnerId(), "Phải có winner");
        boolean winnerInList = false;
        for (int id : bidderIds) {
            if (id == finalAuction.getWinnerId()) { winnerInList = true; break; }
        }
        assertTrue(winnerInList, "Winner phải là 1 trong các bidder đã đặt giá");
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.getInstance().close();
        // Xóa file test database
        java.io.File dbFile = new java.io.File("auction.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
}
