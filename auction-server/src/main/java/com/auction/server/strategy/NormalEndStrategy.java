package com.auction.server.strategy;

import com.auction.common.model.Auction;

/**
 * NormalEndStrategy — không gia hạn.
 * Phiên kết thúc đúng thời gian dự kiến, bất kể bid muộn.
 */
public class NormalEndStrategy implements EndStrategy {

    @Override
    public boolean handleNewBid(Auction auction) {
        // Không làm gì — phiên kết thúc đúng giờ
        return false;
    }

    @Override
    public String getStrategyName() {
        return "Normal End";
    }
}
