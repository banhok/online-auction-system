-- =============================================
-- Online Auction System - Database Schema
-- SQLite
-- =============================================

-- Bảng người dùng
CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    NOT NULL UNIQUE,
    password    TEXT    NOT NULL,
    full_name   TEXT    NOT NULL,
    email       TEXT    NOT NULL UNIQUE,
    role        TEXT    NOT NULL CHECK (role IN ('BIDDER', 'SELLER', 'ADMIN')),
    balance     REAL    NOT NULL DEFAULT 0.0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- Bảng sản phẩm đấu giá
CREATE TABLE IF NOT EXISTS items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    seller_id       INTEGER NOT NULL,
    name            TEXT    NOT NULL,
    description     TEXT,
    category        TEXT    NOT NULL CHECK (category IN ('ELECTRONICS', 'ART', 'VEHICLE')),
    image_url       TEXT,
    starting_price  REAL    NOT NULL,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng phiên đấu giá
CREATE TABLE IF NOT EXISTS auctions (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         INTEGER NOT NULL UNIQUE,
    seller_id       INTEGER NOT NULL,
    starting_price  REAL    NOT NULL,
    current_price   REAL    NOT NULL,
    bid_count       INTEGER NOT NULL DEFAULT 0,
    status          TEXT    NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN', 'RUNNING', 'FINISHED', 'PAID', 'CANCELED')),
    start_time      TEXT    NOT NULL,
    end_time        TEXT    NOT NULL,
    original_end_time TEXT  NOT NULL,
    winner_id       INTEGER,
    deposit_amount  REAL    NOT NULL DEFAULT 0.0,
    anti_snipe_seconds  INTEGER NOT NULL DEFAULT 60,
    anti_snipe_extension INTEGER NOT NULL DEFAULT 60,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (item_id)   REFERENCES items(id)  ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id)  ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(id)  ON DELETE SET NULL
);

-- Bảng giao dịch đặt giá
CREATE TABLE IF NOT EXISTS bid_transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    auction_id  INTEGER NOT NULL,
    bidder_id   INTEGER NOT NULL,
    bid_amount  REAL    NOT NULL,
    bid_time    TEXT    NOT NULL DEFAULT (datetime('now')),
    is_valid    INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    FOREIGN KEY (bidder_id)  REFERENCES users(id)    ON DELETE CASCADE
);

-- Bảng đặt cọc
CREATE TABLE IF NOT EXISTS deposits (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    auction_id  INTEGER NOT NULL,
    amount      REAL    NOT NULL,
    status      TEXT    NOT NULL DEFAULT 'HELD'
                CHECK (status IN ('HELD', 'REFUNDED', 'DEDUCTED', 'FORFEITED')),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    FOREIGN KEY (auction_id) REFERENCES auctions(id) ON DELETE CASCADE,
    UNIQUE(user_id, auction_id)
);

-- Bảng lịch sử giao dịch ví
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    type        TEXT    NOT NULL CHECK (type IN ('DEPOSIT', 'WITHDRAW', 'BID_HOLD', 'BID_REFUND', 'PAYMENT', 'FORFEIT', 'SELLER_DEPOSIT', 'SELLER_REFUND')),
    amount      REAL    NOT NULL,
    description TEXT,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================
-- Indexes cho tối ưu truy vấn
-- =============================================
CREATE INDEX IF NOT EXISTS idx_items_seller      ON items(seller_id);
CREATE INDEX IF NOT EXISTS idx_items_category    ON items(category);
CREATE INDEX IF NOT EXISTS idx_auctions_status   ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_auctions_seller   ON auctions(seller_id);
CREATE INDEX IF NOT EXISTS idx_auctions_end_time ON auctions(end_time);
CREATE INDEX IF NOT EXISTS idx_bids_auction      ON bid_transactions(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder       ON bid_transactions(bidder_id);
CREATE INDEX IF NOT EXISTS idx_deposits_user     ON deposits(user_id);
CREATE INDEX IF NOT EXISTS idx_deposits_auction  ON deposits(auction_id);
CREATE INDEX IF NOT EXISTS idx_wallet_user       ON wallet_transactions(user_id);

-- =============================================
-- Dữ liệu mẫu: Admin mặc định
-- =============================================
INSERT OR IGNORE INTO users (username, password, full_name, email, role, balance)
VALUES ('admin', 'admin123', 'System Admin', 'admin@auction.com', 'ADMIN', 0.0);
