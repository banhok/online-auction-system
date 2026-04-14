package com.auction.server.observer;

import com.auction.common.dto.Response;

/**
 * AuctionEventListener — Observer interface.
 * Mỗi ClientHandler implement interface này để nhận thông báo realtime.
 * Khi có bid mới, phiên kết thúc, gia hạn... tất cả listener được notify.
 */
public interface AuctionEventListener {

    /**
     * Nhận thông báo từ server.
     * @param response Response chứa thông tin event (bid mới, phiên kết thúc...)
     */
    void onEvent(Response response);

    /**
     * Lấy ID của user đang kết nối (dùng để gửi thông báo cá nhân).
     */
    int getUserId();
}
