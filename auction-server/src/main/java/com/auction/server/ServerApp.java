package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.CommandRouter;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.network.SocketServer;
import com.auction.server.observer.AuctionEventManager;
import com.auction.server.service.AuctionScheduler;
import com.auction.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ServerApp — Entry point của Server.
 * Khởi tạo: Database → EventManager → CommandRouter → Scheduler → SocketServer
 */
public class ServerApp {

    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private static final int DEFAULT_PORT = 8888;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port argument, using default: {}", DEFAULT_PORT);
            }
        }

        try {
            // 1. Khởi tạo Database (Singleton)
            logger.info("Initializing database...");
            DatabaseManager.getInstance();

            // 2. Khởi tạo Event Manager (Observer pattern)
            AuctionEventManager eventManager = new AuctionEventManager();

            // 3. Khởi tạo Command Router
            CommandRouter commandRouter = new CommandRouter(eventManager);

            // 4. Dùng CHUNG AuctionService của AuctionController cho Scheduler —
            // để scheduler.finishAuction và commandRouter.placeBid/payForWon
            // chia sẻ cùng map auctionLocks, tránh race khi cùng auctionId.
            AuctionService auctionService = commandRouter.getAuctionController().getAuctionService();

            // 5. Khởi tạo và chạy Scheduler (tự động đóng phiên hết giờ)
            AuctionScheduler scheduler = new AuctionScheduler(auctionService);
            scheduler.start();

            // 6. Đăng ký shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down...");
                scheduler.stop();
                DatabaseManager.getInstance().close();
            }));

            // 7. Khởi tạo và chạy Socket Server (blocking)
            SocketServer server = new SocketServer(port, commandRouter, eventManager);
            server.start();

        } catch (Exception e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        }
    }
}
