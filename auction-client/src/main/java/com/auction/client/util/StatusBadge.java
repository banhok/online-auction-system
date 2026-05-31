package com.auction.client.util;

import com.auction.common.model.AuctionStatus;
import javafx.scene.control.Label;

/**
 * Tạo Label "badge" có màu theo AuctionStatus — dùng cho cellFactory ở TableView.
 * CSS class tương ứng định nghĩa trong style.css: .badge-base + .badge-open/running/finished/paid/canceled/ready.
 */
public final class StatusBadge {

    private StatusBadge() {}

    /** Text tiếng Việt mặc định cho từng status (table Auction). */
    public static String defaultText(AuctionStatus s) {
        if (s == null) return "";
        switch (s) {
            case OPEN:     return "Chờ bắt đầu";
            case RUNNING:  return "Đang đấu giá";
            case FINISHED: return "Chờ thanh toán";
            case PAID:     return "Đã thanh toán";
            case CANCELED: return "Đã huỷ";
            default:       return s.name();
        }
    }

    /** Tạo badge với text custom — caller tự pick text (vd "Đã bán" cho item). */
    public static Label create(AuctionStatus status, String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("badge-base");
        badge.getStyleClass().add(status == null ? "badge-ready" : "badge-" + status.name().toLowerCase());
        return badge;
    }

    /** Tạo badge với text VN mặc định. */
    public static Label create(AuctionStatus status) {
        return create(status, defaultText(status));
    }
}
