package com.auction.common.model;

/**
 * Loại giao dịch ví tiền.
 * DEPOSIT        -> Nạp tiền vào ví
 * WITHDRAW       -> Rút tiền từ ví
 * BID_HOLD       -> Tạm giữ tiền cọc khi tham gia đấu giá
 * BID_REFUND     -> Hoàn tiền cọc khi thua
 * PAYMENT        -> Thanh toán khi thắng đấu giá
 * FORFEIT        -> Mất tiền cọc (thắng nhưng không thanh toán)
 * SELLER_DEPOSIT -> Seller đặt cọc khi đăng sản phẩm
 * SELLER_REFUND  -> Hoàn cọc cho Seller sau phiên kết thúc
 */
public enum WalletTransactionType {
    DEPOSIT,
    WITHDRAW,
    BID_HOLD,
    BID_REFUND,
    PAYMENT,
    FORFEIT,
    SELLER_DEPOSIT,
    SELLER_REFUND
}
