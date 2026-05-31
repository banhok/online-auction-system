package com.auction.server.service;

import com.auction.common.exception.AuthenticationException;
import com.auction.common.exception.NotFoundException;
import com.auction.common.model.*;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho ItemService — quản lý sản phẩm (CRUD + search).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ItemServiceTest {

    private static ItemService itemService;
    private static UserDAO userDAO;
    private static int sellerId;
    private static int otherSellerId;
    private static int itemId;

    @BeforeAll
    static void setup() throws Exception {
        DatabaseManager.resetInstance();
        DatabaseManager.getInstance();
        itemService = new ItemService();
        userDAO = new UserDAO();

        Seller s1 = new Seller("itemtest_seller", "pass123", "Item Seller", "itemseller@test.com");
        s1 = (Seller) userDAO.insert(s1);
        sellerId = s1.getId();

        Seller s2 = new Seller("itemtest_other", "pass123", "Other Seller", "other@test.com");
        s2 = (Seller) userDAO.insert(s2);
        otherSellerId = s2.getId();
    }

    @Test
    @Order(1)
    @DisplayName("Tạo Electronics thành công — dùng Factory")
    void testCreateElectronics() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("brand", "Samsung");
        data.put("condition", "NEW");
        data.put("warrantyMonths", 12);

        Item item = itemService.createItem(sellerId, "Galaxy S24", "Flagship Android",
                "ELECTRONICS", null, 20_000_000, data);

        assertNotNull(item);
        assertTrue(item.getId() > 0);
        itemId = item.getId();
        assertEquals(ItemCategory.ELECTRONICS, item.getCategory());
        assertInstanceOf(Electronics.class, item);
    }

    @Test
    @Order(2)
    @DisplayName("Tạo Art thành công — dùng Factory")
    void testCreateArt() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("artist", "Van Gogh");
        data.put("year", 1889);
        data.put("medium", "Oil");

        Item item = itemService.createItem(sellerId, "Starry Night", "Masterpiece",
                "ART", null, 999_999_999, data);

        assertEquals(ItemCategory.ART, item.getCategory());
        assertInstanceOf(Art.class, item);
    }

    @Test
    @Order(3)
    @DisplayName("Tạo với tên rỗng → exception")
    void testCreateEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(sellerId, "  ", "desc", "ELECTRONICS", null, 100000, null));
    }

    @Test
    @Order(4)
    @DisplayName("Tạo với startingPrice âm → exception")
    void testCreateNegativePrice() {
        assertThrows(IllegalArgumentException.class, () ->
                itemService.createItem(sellerId, "Bad", "d", "ELECTRONICS", null, -1, null));
    }

    @Test
    @Order(5)
    @DisplayName("Lấy item theo id thành công")
    void testGetItem() throws Exception {
        Item item = itemService.getItem(itemId);
        assertNotNull(item);
        assertEquals("Galaxy S24", item.getName());
    }

    @Test
    @Order(6)
    @DisplayName("Lấy item không tồn tại → NotFoundException")
    void testGetItemNotFound() {
        assertThrows(NotFoundException.class, () -> itemService.getItem(99999));
    }

    @Test
    @Order(7)
    @DisplayName("Cập nhật item bởi đúng seller")
    void testUpdateItem() throws Exception {
        Item updated = itemService.updateItem(itemId, sellerId,
                "Galaxy S24 Ultra", "Updated desc", null, 22_000_000);

        assertEquals("Galaxy S24 Ultra", updated.getName());
        assertEquals(22_000_000, updated.getStartingPrice());
    }

    @Test
    @Order(8)
    @DisplayName("Cập nhật item bởi seller khác → AuthenticationException")
    void testUpdateItemUnauthorized() {
        assertThrows(AuthenticationException.class, () ->
                itemService.updateItem(itemId, otherSellerId, "Hack", null, null, 1));
    }

    @Test
    @Order(9)
    @DisplayName("Lấy items theo seller")
    void testGetItemsBySeller() throws Exception {
        List<Item> items = itemService.getItemsBySeller(sellerId);
        assertTrue(items.size() >= 2);
    }

    @Test
    @Order(10)
    @DisplayName("Search theo keyword")
    void testSearchByKeyword() throws Exception {
        List<Item> items = itemService.searchItems("Galaxy", null);
        assertTrue(items.stream().anyMatch(i -> i.getName().contains("Galaxy")));
    }

    @Test
    @Order(11)
    @DisplayName("Search theo category")
    void testSearchByCategory() throws Exception {
        List<Item> items = itemService.searchItems(null, "ART");
        assertTrue(items.stream().allMatch(i -> i.getCategory() == ItemCategory.ART));
    }

    @Test
    @Order(12)
    @DisplayName("Xóa item bởi seller khác → AuthenticationException")
    void testDeleteItemUnauthorized() {
        assertThrows(AuthenticationException.class, () ->
                itemService.deleteItem(itemId, otherSellerId));
    }

    @Test
    @Order(13)
    @DisplayName("Xóa item thành công")
    void testDeleteItem() throws Exception {
        itemService.deleteItem(itemId, sellerId);
        assertThrows(NotFoundException.class, () -> itemService.getItem(itemId));
    }

    @AfterAll
    static void cleanup() {
        DatabaseManager.getInstance().close();
        java.io.File dbFile = new java.io.File("auction.db");
        if (dbFile.exists()) dbFile.delete();
    }
}
