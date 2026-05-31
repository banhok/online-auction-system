package com.auction.common.exception;

/**
 * Ném khi xác thực/ủy quyền thất bại: sai username/password,
 * session invalid, hoặc user không có quyền thao tác trên tài nguyên.
 */
public class AuthenticationException extends AuctionException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
