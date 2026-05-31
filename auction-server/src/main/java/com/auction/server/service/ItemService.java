package com.auction.server.service;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.exception.NotFoundException;
import com.auction.common.model.*;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.factory.ItemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * ItemService — xử lý logic sản phẩm đấu giá.
 * Sử dụng ItemFactory để tạo đúng loại Item.
 */
public class ItemService {

    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();

    /**
     * Tạo sản phẩm mới (Seller).
     * Sử dụng Factory Method pattern qua ItemFactory.
     */
    public Item createItem(int sellerId, String name, String description,
                           String categoryStr, String imageUrl, double startingPrice,
                           Map<String, Object> extraData) throws Exception {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (startingPrice <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải lớn hơn 0");
        }

        ItemCategory category = ItemCategory.valueOf(categoryStr.toUpperCase());

        // Factory Method — tạo đúng lớp con dựa trên category
        Item item = ItemFactory.create(category, sellerId, name, description,
                imageUrl, startingPrice, extraData);

        return itemDAO.insert(item);
    }

    /**
     * Cập nhật sản phẩm.
     */
    public Item updateItem(int itemId, int sellerId, String name, String description,
                           String imageUrl, double startingPrice) throws Exception {

        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));

        if (item.getSellerId() != sellerId) {
            throw new AuthenticationException("Bạn không có quyền sửa sản phẩm này");
        }

        if (name != null) item.setName(name);
        if (description != null) item.setDescription(description);
        if (imageUrl != null) item.setImageUrl(imageUrl);
        if (startingPrice > 0) item.setStartingPrice(startingPrice);

        itemDAO.update(item);
        return item;
    }

    /**
     * Xóa sản phẩm.
     * Chặn nếu sản phẩm đang có phiên đấu giá — tránh cascade xóa auction + bids
     * (FK auctions.item_id ON DELETE CASCADE trong schema.sql).
     */
    public void deleteItem(int itemId, int sellerId) throws Exception {
        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));

        if (item.getSellerId() != sellerId) {
            throw new AuthenticationException("Bạn không có quyền xóa sản phẩm này");
        }

        if (auctionDAO.findByItemId(itemId).isPresent()) {
            throw new IllegalArgumentException(
                    "Sản phẩm đang có phiên đấu giá — huỷ phiên trước khi xoá");
        }

        itemDAO.delete(itemId);
    }

    /**
     * Lấy sản phẩm theo id.
     */
    public Item getItem(int itemId) throws Exception {
        return itemDAO.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));
    }

    /**
     * Lấy sản phẩm theo seller.
     */
    public List<Item> getItemsBySeller(int sellerId) throws Exception {
        return itemDAO.findBySellerId(sellerId);
    }

    /**
     * Tìm kiếm sản phẩm.
     */
    public List<Item> searchItems(String keyword, String categoryStr) throws Exception {
        if (categoryStr != null && !categoryStr.isEmpty()) {
            ItemCategory category = ItemCategory.valueOf(categoryStr.toUpperCase());
            return itemDAO.findByCategory(category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            return itemDAO.searchByName(keyword);
        }
        return itemDAO.searchByName("");
    }
}
