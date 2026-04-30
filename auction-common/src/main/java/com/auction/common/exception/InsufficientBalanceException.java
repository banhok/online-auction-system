package com.auction.common.exception;

/**
 * Ném khi số dư ví không đủ để thực hiện giao dịch:
 * rút tiền, đặt cọc, thanh toán sau khi thắng phiên.
 */
public class InsufficientBalanceException extends AuctionException {

    private static final long serialVersionUID = 1L;

    public InsufficientBalanceException(String message) {
        super(message);
    }

    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
