package com.auction.server.factory;

import com.auction.common.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test cho ItemFactory — Factory Method pattern.
 * Đảm bảo đúng lớp con được tạo theo category + default values khi data rỗng.
 */
class ItemFactoryTest {

    @Test
    @DisplayName("Tạo Electronics với đầy đủ dữ liệu")
    void testCreateElectronicsFull() {
        Map<String, Object> data = new HashMap<>();
        data.put("brand", "Apple");
        data.put("condition", "NEW");
        data.put("warrantyMonths", 24);

        Item item = ItemFactory.create(ItemCategory.ELECTRONICS, 1, "iPhone 15",
                "Flagship phone", null, 25_000_000, data);

        assertInstanceOf(Electronics.class, item);
        Electronics e = (Electronics) item;
        assertEquals("Apple", e.getBrand());
        assertEquals("NEW", e.getCondition());
        assertEquals(24, e.getWarrantyMonths());
    }

    @Test
    @DisplayName("Tạo Electronics với data null → dùng default")
    void testCreateElectronicsDefaults() {
        Item item = ItemFactory.create(ItemCategory.ELECTRONICS, 1, "No brand",
                null, null, 100_000, null);

        assertInstanceOf(Electronics.class, item);
        Electronics e = (Electronics) item;
        assertEquals("Unknown", e.getBrand());
        assertEquals("USED", e.getCondition());
        assertEquals(0, e.getWarrantyMonths());
    }

    @Test
    @DisplayName("Tạo Art với year dạng String trong data")
    void testCreateArtWithStringYear() {
        Map<String, Object> data = new HashMap<>();
        data.put("artist", "Monet");
        data.put("year", "1870");
        data.put("medium", "Oil on canvas");

        Item item = ItemFactory.create(ItemCategory.ART, 1, "Water Lilies",
                null, null, 1_000_000, data);

        assertInstanceOf(Art.class, item);
        Art a = (Art) item;
        assertEquals("Monet", a.getArtist());
        assertEquals(1870, a.getYear());
        assertEquals("Oil on canvas", a.getMedium());
    }

    @Test
    @DisplayName("Tạo Art với year không parse được → fallback default")
    void testCreateArtWithInvalidYear() {
        Map<String, Object> data = new HashMap<>();
        data.put("year", "not-a-number");
        Item item = ItemFactory.create(ItemCategory.ART, 1, "Untitled",
                null, null, 500_000, data);

        Art a = (Art) item;
        assertEquals(2024, a.getYear());
    }

    @Test
    @DisplayName("Tạo Vehicle đầy đủ")
    void testCreateVehicleFull() {
        Map<String, Object> data = new HashMap<>();
        data.put("make", "Toyota");
        data.put("modelName", "Camry");
        data.put("yearMade", 2022);
        data.put("mileage", 12000);

        Item item = ItemFactory.create(ItemCategory.VEHICLE, 1, "Sedan",
                null, null, 800_000_000, data);

        assertInstanceOf(Vehicle.class, item);
        Vehicle v = (Vehicle) item;
        assertEquals("Toyota", v.getMake());
        assertEquals("Camry", v.getModelName());
        assertEquals(2022, v.getYearMade());
        assertEquals(12000, v.getMileage());
    }

    @Test
    @DisplayName("getCategoryDetails() polymorphism trả về đúng string cho mỗi subclass")
    void testPolymorphismGetCategoryDetails() {
        Item electronics = ItemFactory.create(ItemCategory.ELECTRONICS, 1, "E", null, null, 1, null);
        Item art = ItemFactory.create(ItemCategory.ART, 1, "A", null, null, 1, null);
        Item vehicle = ItemFactory.create(ItemCategory.VEHICLE, 1, "V", null, null, 1, null);

        assertNotNull(electronics.getCategoryDetails());
        assertNotNull(art.getCategoryDetails());
        assertNotNull(vehicle.getCategoryDetails());
        // 3 string khác nhau — polymorphism thực sự
        assertNotEquals(electronics.getCategoryDetails(), art.getCategoryDetails());
        assertNotEquals(art.getCategoryDetails(), vehicle.getCategoryDetails());
    }
}
