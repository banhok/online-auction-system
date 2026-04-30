package com.auction.common.exception;

/**
 * Ném khi bid không hợp lệ: giá thấp hơn currentPrice, phiên không RUNNING,
 * bidder chưa đặt cọc, seller tự bid sản phẩm mình, phiên đã hết giờ.
 */
public class InvalidBidException extends AuctionException {

    private static final long serialVersionUID = 1L;

    public InvalidBidException(String message) {
        super(message);
    }

    public InvalidBidException(String message, Throwable cause) {
        super(message, cause);
    }
}
