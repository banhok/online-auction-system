package com.auction.common.model;

/**
 * Trạng thái tiền đặt cọc.
 * HELD     -> Đang bị tạm giữ trong phiên đấu giá
 * REFUNDED -> Đã hoàn trả (thua đấu giá)
 * DEDUCTED -> Đã trừ vào thanh toán (thắng đấu giá)
 */
public enum DepositStatus {
    HELD,
    REFUNDED,
    DEDUCTED
}
