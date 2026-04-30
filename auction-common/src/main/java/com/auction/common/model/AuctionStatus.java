package com.auction.common.model;

/**
 * Trạng thái của phiên đấu giá.
 * OPEN     -> Vừa tạo, chưa bắt đầu
 * RUNNING  -> Đang diễn ra
 * FINISHED -> Đã kết thúc, chờ thanh toán
 * PAID     -> Người thắng đã thanh toán
 * CANCELED -> Bị hủy (không có ai bid hoặc không thanh toán)
 */
public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}
