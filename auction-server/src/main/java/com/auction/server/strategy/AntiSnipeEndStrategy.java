package com.auction.server.strategy;

import com.auction.common.model.Auction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AntiSnipeEndStrategy — gia hạn phiên khi có bid gần cuối.
 * Nếu bid mới nằm trong vùng anti-snipe (X giây cuối),
 * tự động gia hạn thêm Y giây.
 * Ngăn chặn chiến thuật "snipe" (đặt giá vào giây cuối).
 */
public class AntiSnipeEndStrategy implements EndStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AntiSnipeEndStrategy.class);

    @Override
    public boolean handleNewBid(Auction auction) {
        if (auction.isInAntiSnipeZone()) {
            auction.extendEndTime();
            logger.info("Anti-snipe triggered for auction #{}: extended to {}",
                    auction.getId(), auction.getEndTime());
            return true; // Đã gia hạn
        }
        return false; // Không cần gia hạn
    }

    @Override
    public String getStrategyName() {
        return "Anti-Snipe End";
    }
}
