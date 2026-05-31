package com.auction.common.exception;

/**
 * Base class cho tất cả lỗi domain của hệ thống đấu giá.
 * Extend RuntimeException để không buộc khai báo throws mọi nơi.
 * Không tạo instance trực tiếp — dùng subclass tương ứng với loại lỗi.
 */
public abstract class AuctionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected AuctionException(String message) {
        super(message);
    }

    protected AuctionException(String message, Throwable cause) {
        super(message, cause);
    }
}
