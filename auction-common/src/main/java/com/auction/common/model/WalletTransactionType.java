package com.auction.common.model;

/**
 * Loại giao dịch ví tiền.
 * DEPOSIT    -> Nạp tiền vào ví (hoặc seller nhận thanh toán từ winner)
 * WITHDRAW   -> Rút tiền từ ví
 * BID_HOLD   -> Tạm giữ tiền cọc khi tham gia đấu giá
 * BID_REFUND -> Hoàn tiền cọc khi thua
 * PAYMENT    -> Thanh toán khi thắng đấu giá
 */
public enum WalletTransactionType {
    DEPOSIT,
    WITHDRAW,
    BID_HOLD,
    BID_REFUND,
    PAYMENT
}
