package com.auction.server.network;

import com.auction.server.controller.CommandRouter;
import com.auction.server.observer.AuctionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SocketServer — lắng nghe kết nối từ Client.
 * Mỗi client kết nối → tạo 1 ClientHandler chạy trên thread riêng.
 * Dùng ThreadPool để giới hạn số thread đồng thời.
 */
public class SocketServer {

    private static final Logger logger = LoggerFactory.getLogger(SocketServer.class);
    private static final int MAX_CLIENTS = 50;

    private final int port;
    private final CommandRouter commandRouter;
    private final AuctionEventManager eventManager;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public SocketServer(int port, CommandRouter commandRouter, AuctionEventManager eventManager) {
        this.port = port;
        this.commandRouter = commandRouter;
        this.eventManager = eventManager;
    }

    /**
     * Bắt đầu lắng nghe kết nối.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        running = true;

        logger.info("=== Auction Server started on port {} ===", port);
        logger.info("Waiting for clients...");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: {}", clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, commandRouter, eventManager);
                threadPool.execute(handler);

            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting client connection", e);
                }
            }
        }
    }

    /**
     * Dừng server.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            logger.info("Server stopped.");
        } catch (IOException e) {
            logger.error("Error stopping server", e);
        }
    }
}
