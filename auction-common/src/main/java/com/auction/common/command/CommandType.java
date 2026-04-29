package com.auction.common.command;

/**
 * CommandType — danh sách tất cả loại lệnh Client gửi tới Server.
 * Dùng trong Command Pattern: mỗi request là 1 command.
 */
public enum CommandType {

    // --- User ---
    LOGIN,
    REGISTER,
    GET_PROFILE,
    UPDATE_PROFILE,
    CHANGE_PASSWORD,
    GET_ALL_USERS,       // Admin
    DELETE_USER,         // Admin

    // --- Wallet ---
    GET_BALANCE,
    TOP_UP,              // Nạp tiền
    WITHDRAW,            // Rút tiền
    GET_WALLET_HISTORY,

    // --- Item ---
    CREATE_ITEM,
    UPDATE_ITEM,
    DELETE_ITEM,
    GET_ITEM,
    GET_ITEMS_BY_SELLER,
    SEARCH_ITEMS,

    // --- Auction ---
    CREATE_AUCTION,
    GET_AUCTION,
    GET_ALL_AUCTIONS,
    GET_AUCTIONS_BY_STATUS,
    GET_AUCTIONS_BY_SELLER,
    GET_MY_WINS,         // Bidder xem phiên mình đã thắng (FINISHED hoặc PAID)

    // --- Bidding ---
    PLACE_BID,
    GET_BID_HISTORY,
    JOIN_AUCTION,        // Đặt cọc + theo dõi
    PAY_WINNER,          // Winner thanh toán sau khi thắng (FINISHED → PAID)
    CANCEL_AUCTION,      // Seller huỷ phiên — refund toàn bộ cọc (chỉ khi OPEN/RUNNING)

    // --- Realtime ---
    SUBSCRIBE_AUCTION,   // Đăng ký nhận update realtime
    UNSUBSCRIBE_AUCTION, // Hủy đăng ký

    // --- Notification từ Server ---
    BID_UPDATE,          // Server thông báo có bid mới
    AUCTION_ENDED,       // Server thông báo phiên kết thúc
    AUCTION_EXTENDED,    // Server thông báo gia hạn (anti-snipe)
    DEPOSIT_REFUNDED,    // Server thông báo hoàn cọc
    BALANCE_UPDATE,      // Server thông báo balance user thay đổi (refund/payment/credit/...)

    // --- System ---
    PING,
    PONG,
    ERROR
}
