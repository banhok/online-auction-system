package com.auction.server.network;

import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.util.JsonUtil;
import com.auction.server.controller.CommandRouter;
import com.auction.server.observer.AuctionEventListener;
import com.auction.server.observer.AuctionEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * ClientHandler — xử lý 1 client kết nối.
 * Implements AuctionEventListener để nhận thông báo realtime (Observer pattern).
 * Mỗi client chạy trên 1 thread riêng.
 * Đọc JSON request → gọi CommandRouter → gửi JSON response.
 */
public class ClientHandler implements Runnable, AuctionEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final CommandRouter commandRouter;
    private final AuctionEventManager eventManager;
    private BufferedReader reader;
    private PrintWriter writer;
    private int userId = -1; // -1 = chưa đăng nhập
    private volatile boolean connected = true;

    public ClientHandler(Socket socket, CommandRouter commandRouter, AuctionEventManager eventManager) {
        this.socket = socket;
        this.commandRouter = commandRouter;
        this.eventManager = eventManager;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Đăng ký global listener
            eventManager.registerGlobal(this);

            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    // Parse JSON request
                    Request request = JsonUtil.parseRequest(line);
                    logger.debug("Received from user {}: {}", userId, request.getCommand());

                    // Gắn userId vào request (nếu đã đăng nhập)
                    if (userId > 0) {
                        request.put("_userId", userId);
                    }

                    // Gắn reference tới handler (để subscribe auction)
                    request.put("_handler", this);

                    // Route command và lấy response
                    Response response = commandRouter.route(request);

                    // Nếu login/register thành công → cập nhật userId
                    if (response.isSuccess() && response.get("userId") != null) {
                        Object uid = response.get("userId");
                        if (uid instanceof Number) {
                            this.userId = ((Number) uid).intValue();
                        }
                    }

                    // Gửi response
                    sendResponse(response);

                } catch (Exception e) {
                    logger.error("Error processing request from user {}", userId, e);
                    sendResponse(Response.error(null, "Lỗi server: " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            logger.info("Client disconnected: user {}", userId);
        } finally {
            disconnect();
        }
    }

    /**
     * Gửi response tới client (JSON, 1 dòng).
     */
    public synchronized void sendResponse(Response response) {
        if (writer != null && connected) {
            String json = JsonUtil.toJson(response);
            // Gửi trên 1 dòng (loại bỏ newline trong pretty-print)
            writer.println(json.replace("\n", "").replace("\r", ""));
        }
    }

    /**
     * Ngắt kết nối.
     */
    private void disconnect() {
        connected = false;
        eventManager.unregisterGlobal(this);
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.error("Error closing connection for user {}", userId, e);
        }
        logger.info("User {} disconnected and cleaned up.", userId);
    }

    // --- AuctionEventListener implementation (Observer) ---

    @Override
    public void onEvent(Response response) {
        sendResponse(response);
    }

    @Override
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public AuctionEventManager getEventManager() {
        return eventManager;
    }
}
