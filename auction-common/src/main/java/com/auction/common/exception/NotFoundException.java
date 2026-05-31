package com.auction.common.exception;

/**
 * Ném khi entity không tồn tại trong database: user, item, auction, deposit.
 */
public class NotFoundException extends AuctionException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
