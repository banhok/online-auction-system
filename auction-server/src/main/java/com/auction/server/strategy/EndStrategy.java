package com.auction.server.strategy;

import com.auction.common.model.Auction;

/**
 * EndStrategy — Strategy pattern interface.
 * Định nghĩa cách xử lý khi có bid mới gần cuối phiên.
 * Có 2 chiến lược: NormalEnd (không gia hạn) và AntiSnipeEnd (gia hạn).
 */
public interface EndStrategy {

    /**
     * Xử lý sau khi có bid mới — kiểm tra có cần gia hạn không.
     * @param auction Phiên đấu giá hiện tại
     * @return true nếu đã gia hạn thời gian, false nếu không
     */
    boolean handleNewBid(Auction auction);

    /**
     * Lấy tên chiến lược (để log/debug).
     */
    String getStrategyName();
}
