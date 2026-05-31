package com.auction.client.network;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * ServerConnection — Singleton quản lý kết nối tới Server.
 * Gửi Request (JSON) và nhận Response (JSON) qua Socket.
 * Hỗ trợ nhận thông báo realtime từ server (listener pattern phía client).
 */
public class ServerConnection {

    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);
    private static final int TIMEOUT_SECONDS = 10;

    private static volatile ServerConnection instance;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected = false;
    private Thread listenerThread;

    // Callback cho response đồng bộ (request-response).
    // BẮT BUỘC chỉ truy cập trong synchronized block của sendRequest —
    // các trường này không hỗ trợ nhiều request concurrent (sẽ giẫm lên nhau).
    private Response pendingResponse;
    private CountDownLatch responseLatch;
    private final Object requestLock = new Object();

    // Listeners cho thông báo realtime (async)
    private final List<Consumer<Response>> eventListeners = new CopyOnWriteArrayList<>();

    // Thông tin user đang đăng nhập
    private int currentUserId = -1;
    private String currentUsername;
    private String currentRole;
    private volatile double currentBalance;

    private ServerConnection() {
    }

    public static ServerConnection getInstance() {
        if (instance == null) {
            synchronized (ServerConnection.class) {
                if (instance == null) {
                    instance = new ServerConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Kết nối tới server.
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        connected = true;

        // Thread lắng nghe response từ server
        listenerThread = new Thread(this::listenForResponses, "ServerListener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        logger.info("Connected to server {}:{}", host, port);
    }

    /**
     * Lắng nghe response từ server (chạy trên thread riêng).
     */
    private void listenForResponses() {
        try {
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    Response response = JsonUtil.parseResponse(line);

                    // Kiểm tra đây là response cho request đang chờ hay thông báo realtime
                    if (isRealtimeEvent(response)) {
                        // Thông báo realtime → gọi tất cả event listeners
                        for (Consumer<Response> listener : eventListeners) {
                            try {
                                listener.accept(response);
                            } catch (Exception e) {
                                logger.error("Error in event listener", e);
                            }
                        }
                    } else {
                        // Response cho request đang chờ
                        pendingResponse = response;
                        if (responseLatch != null) {
                            responseLatch.countDown();
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error parsing response", e);
                }
            }
        } catch (IOException e) {
            if (connected) {
                logger.error("Connection lost", e);
            }
        } finally {
            connected = false;
        }
    }

    /**
     * Kiểm tra response có phải thông báo realtime không.
     */
    private boolean isRealtimeEvent(Response response) {
        if (response.getCommand() == null) return false;
        switch (response.getCommand()) {
            case BID_UPDATE:
            case AUCTION_ENDED:
            case AUCTION_EXTENDED:
            case DEPOSIT_REFUNDED:
            case BALANCE_UPDATE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Gửi request và chờ response (đồng bộ).
     * Synchronize qua requestLock — giao thức hiện không có requestId để match
     * response → mỗi lúc chỉ 1 request-response pair được in-flight; nếu không
     * các latch/pendingResponse sẽ giẫm lên nhau gây timeout giả.
     */
    public Response sendRequest(Request request) throws Exception {
        if (!connected || writer == null) {
            throw new IOException("Chưa kết nối tới server");
        }

        synchronized (requestLock) {
            responseLatch = new CountDownLatch(1);
            pendingResponse = null;

            String json = JsonUtil.toJson(request);
            writer.println(json.replace("\n", "").replace("\r", ""));

            boolean received = responseLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!received || pendingResponse == null) {
                throw new IOException("Server không phản hồi (timeout)");
            }

            return pendingResponse;
        }
    }

    /**
     * Gửi request nhanh — tạo Request từ command + data.
     */
    public Response send(CommandType command) throws Exception {
        return sendRequest(new Request(command));
    }

    public Response send(CommandType command, String key, Object value) throws Exception {
        Request req = new Request(command);
        req.put(key, value);
        return sendRequest(req);
    }

    public Response send(CommandType command, Request request) throws Exception {
        request.setCommand(command);
        return sendRequest(request);
    }

    /**
     * Đăng ký listener nhận thông báo realtime.
     */
    public void addEventListener(Consumer<Response> listener) {
        eventListeners.add(listener);
    }

    /**
     * Hủy đăng ký listener.
     */
    public void removeEventListener(Consumer<Response> listener) {
        eventListeners.remove(listener);
    }

    /**
     * Ngắt kết nối.
     */
    public void disconnect() {
        connected = false;
        try {
            //if (reader != null) reader.close();
            //if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.error("Error disconnecting", e);
        }
        currentUserId = -1;
        currentUsername = null;
        logger.info("Disconnected from server.");
    }

    // --- Session info ---

    public boolean isConnected() {
        return connected;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(int id) {
        this.currentUserId = id;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String role) {
        this.currentRole = role;
    }

    public double getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(double balance) {
        this.currentBalance = balance;
    }

    public boolean isLoggedIn() {
        return currentUserId > 0;
    }
}
