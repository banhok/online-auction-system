package com.auction.server.service;

import com.auction.common.model.Auction;
import com.auction.common.model.AuctionStatus;
import com.auction.server.dao.AuctionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * AuctionScheduler — chạy nền để tự động:
 * 1. Chuyển trạng thái OPEN -> RUNNING khi đến giờ bắt đầu
 * 2. Đóng phiên RUNNING -> FINISHED khi hết giờ
 * Chạy mỗi 2 giây kiểm tra.
 */
public class AuctionScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AuctionScheduler.class);
    private static final int CHECK_INTERVAL_SECONDS = 2;

    private final AuctionService auctionService;
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private ScheduledExecutorService scheduler;

    public AuctionScheduler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Bắt đầu scheduler.
     */
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionScheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::checkAuctions,
                0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);

        logger.info("AuctionScheduler started (interval: {}s)", CHECK_INTERVAL_SECONDS);
    }

    /**
     * Dừng scheduler.
     */
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            logger.info("AuctionScheduler stopped.");
        }
    }

    /**
     * Kiểm tra và xử lý các phiên đấu giá.
     */
    private void checkAuctions() {
        try {
            // 1. Chuyển OPEN -> RUNNING nếu đến giờ bắt đầu
            activateOpenAuctions();

            // 2. Đóng RUNNING -> FINISHED nếu hết giờ
            closeExpiredAuctions();

        } catch (Exception e) {
            logger.error("Error in AuctionScheduler", e);
        }
    }

    /**
     * Kích hoạt các phiên OPEN đã đến giờ bắt đầu.
     */
    private void activateOpenAuctions() throws Exception {
        List<Auction> openAuctions = auctionDAO.findByStatus(AuctionStatus.OPEN);
        for (Auction auction : openAuctions) {
            if (auction.getStartTime() != null &&
                !auction.getStartTime().isAfter(java.time.LocalDateTime.now())) {
                auction.setStatus(AuctionStatus.RUNNING);
                auctionDAO.updateStatus(auction.getId(), AuctionStatus.RUNNING);
                logger.info("Auction #{} activated: OPEN -> RUNNING", auction.getId());
            }
        }
    }

    /**
     * Đóng các phiên RUNNING đã hết giờ.
     */
    private void closeExpiredAuctions() throws Exception {
        List<Auction> expired = auctionService.getExpiredRunning();
        for (Auction auction : expired) {
            try {
                auctionService.finishAuction(auction.getId());
                logger.info("Auction #{} closed by scheduler", auction.getId());
            } catch (Exception e) {
                logger.error("Failed to close auction #{}", auction.getId(), e);
            }
        }
    }
}
