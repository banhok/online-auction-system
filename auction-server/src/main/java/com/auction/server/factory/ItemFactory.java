package com.auction.server.factory;

import com.auction.common.model.*;

import java.util.Map;

/**
 * ItemFactory — Factory Method pattern.
 * Tạo đúng lớp con Item (Electronics/Art/Vehicle) dựa trên category.
 * Client không cần biết cụ thể lớp con nào được tạo ra,
 * chỉ cần gọi ItemFactory.create() với category và data.
 */
public class ItemFactory {

    // Private constructor — utility class, không cho tạo instance
    private ItemFactory() {
    }

    /**
     * Factory Method chính — tạo Item dựa trên category.
     *
     * @param category   Loại sản phẩm (ELECTRONICS, ART, VEHICLE)
     * @param sellerId   ID người bán
     * @param name       Tên sản phẩm
     * @param description Mô tả
     * @param imageUrl   URL ảnh
     * @param startingPrice Giá khởi điểm
     * @param extraData  Dữ liệu bổ sung theo từng loại (key-value)
     * @return Item đúng lớp con
     */
    public static Item create(ItemCategory category, int sellerId, String name,
                              String description, String imageUrl, double startingPrice,
                              Map<String, Object> extraData) {

        switch (category) {
            case ELECTRONICS:
                return createElectronics(sellerId, name, description, imageUrl,
                        startingPrice, extraData);

            case ART:
                return createArt(sellerId, name, description, imageUrl,
                        startingPrice, extraData);

            case VEHICLE:
                return createVehicle(sellerId, name, description, imageUrl,
                        startingPrice, extraData);

            default:
                throw new IllegalArgumentException("Unknown item category: " + category);
        }
    }

    /**
     * Tạo Electronics.
     */
    private static Electronics createElectronics(int sellerId, String name, String description,
                                                  String imageUrl, double startingPrice,
                                                  Map<String, Object> data) {
        String brand = getStringOrDefault(data, "brand", "Unknown");
        String condition = getStringOrDefault(data, "condition", "USED");
        int warranty = getIntOrDefault(data, "warrantyMonths", 0);

        return new Electronics(sellerId, name, description, imageUrl,
                startingPrice, brand, condition, warranty);
    }

    /**
     * Tạo Art.
     */
    private static Art createArt(int sellerId, String name, String description,
                                  String imageUrl, double startingPrice,
                                  Map<String, Object> data) {
        String artist = getStringOrDefault(data, "artist", "Unknown");
        int year = getIntOrDefault(data, "year", 2024);
        String medium = getStringOrDefault(data, "medium", "Mixed");

        return new Art(sellerId, name, description, imageUrl,
                startingPrice, artist, year, medium);
    }

    /**
     * Tạo Vehicle.
     */
    private static Vehicle createVehicle(int sellerId, String name, String description,
                                          String imageUrl, double startingPrice,
                                          Map<String, Object> data) {
        String make = getStringOrDefault(data, "make", "Unknown");
        String modelName = getStringOrDefault(data, "modelName", "Unknown");
        int yearMade = getIntOrDefault(data, "yearMade", 2020);
        int mileage = getIntOrDefault(data, "mileage", 0);

        return new Vehicle(sellerId, name, description, imageUrl,
                startingPrice, make, modelName, yearMade, mileage);
    }

    // --- Helper methods ---

    private static String getStringOrDefault(Map<String, Object> data, String key, String defaultVal) {
        if (data == null || !data.containsKey(key)) return defaultVal;
        Object val = data.get(key);
        return val != null ? val.toString() : defaultVal;
    }

    private static int getIntOrDefault(Map<String, Object> data, String key, int defaultVal) {
        if (data == null || !data.containsKey(key)) return defaultVal;
        Object val = data.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
