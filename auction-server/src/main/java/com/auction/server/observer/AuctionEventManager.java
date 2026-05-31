package com.auction.server.observer;

import com.auction.common.dto.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AuctionEventManager — Observer pattern manager.
 * Quản lý danh sách listener theo từng phiên đấu giá.
 * Khi có event → notify tất cả listener đang subscribe phiên đó.
 * Thread-safe nhờ ConcurrentHashMap + CopyOnWriteArrayList.
 */
public class AuctionEventManager {

    private static final Logger logger = LoggerFactory.getLogger(AuctionEventManager.class);

    // Map: auctionId -> danh sách listener đang theo dõi phiên đó
    private final Map<Integer, List<AuctionEventListener>> subscribers = new ConcurrentHashMap<>();

    // Danh sách tất cả listener (dùng cho broadcast toàn hệ thống)
    private final List<AuctionEventListener> globalListeners = new CopyOnWriteArrayList<>();

    /**
     * Đăng ký listener vào hệ thống (khi client kết nối).
     */
    public void registerGlobal(AuctionEventListener listener) {
        globalListeners.add(listener);
        logger.info("Global listener registered: user {}", listener.getUserId());
    }

    /**
     * Hủy đăng ký listener khỏi hệ thống (khi client ngắt kết nối).
     */
    public void unregisterGlobal(AuctionEventListener listener) {
        globalListeners.remove(listener);
        // Xóa khỏi tất cả auction subscriptions
        subscribers.values().forEach(list -> list.remove(listener));
        logger.info("Global listener unregistered: user {}", listener.getUserId());
    }

    /**
     * Subscribe listener vào 1 phiên đấu giá cụ thể.
     */
    public void subscribe(int auctionId, AuctionEventListener listener) {
        subscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>());
        List<AuctionEventListener> list = subscribers.get(auctionId);
        if (!list.contains(listener)) {
            list.add(listener);
            logger.info("User {} subscribed to auction {}", listener.getUserId(), auctionId);
        }
    }

    /**
     * Unsubscribe listener khỏi 1 phiên đấu giá.
     */
    public void unsubscribe(int auctionId, AuctionEventListener listener) {
        List<AuctionEventListener> list = subscribers.get(auctionId);
        if (list != null) {
            list.remove(listener);
            logger.info("User {} unsubscribed from auction {}", listener.getUserId(), auctionId);
        }
    }

    /**
     * Gửi thông báo tới tất cả listener đang theo dõi 1 phiên đấu giá.
     */
    public void notifyAuction(int auctionId, Response response) {
        List<AuctionEventListener> list = subscribers.get(auctionId);
        if (list != null) {
            for (AuctionEventListener listener : list) {
                try {
                    listener.onEvent(response);
                } catch (Exception e) {
                    logger.error("Failed to notify user {} for auction {}",
                            listener.getUserId(), auctionId, e);
                }
            }
        }
    }

    /**
     * Gửi thông báo cá nhân tới 1 user cụ thể.
     */
    public void notifyUser(int userId, Response response) {
        for (AuctionEventListener listener : globalListeners) {
            if (listener.getUserId() == userId) {
                try {
                    listener.onEvent(response);
                } catch (Exception e) {
                    logger.error("Failed to notify user {}", userId, e);
                }
                return;
            }
        }
    }

    /**
     * Broadcast thông báo tới toàn bộ client.
     */
    public void broadcast(Response response) {
        for (AuctionEventListener listener : globalListeners) {
            try {
                listener.onEvent(response);
            } catch (Exception e) {
                logger.error("Failed to broadcast to user {}", listener.getUserId(), e);
            }
        }
    }

    /**
     * Lấy số lượng subscriber của 1 phiên.
     */
    public int getSubscriberCount(int auctionId) {
        List<AuctionEventListener> list = subscribers.get(auctionId);
        return list != null ? list.size() : 0;
    }

    /**
     * Xóa tất cả subscriber của 1 phiên (khi phiên kết thúc).
     */
    public void clearAuction(int auctionId) {
        subscribers.remove(auctionId);
    }
}
