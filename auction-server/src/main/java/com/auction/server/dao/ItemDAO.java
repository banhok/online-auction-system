package com.auction.server.dao;

import com.auction.common.model.*;
import com.auction.common.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ItemDAO — Data Access Object cho Item.
 * Lưu ý: các thuộc tính riêng của từng loại (brand, artist, make...)
 * được lưu trong cột description dưới dạng JSON phụ.
 *
 * THREAD-SAFETY: Mọi method bọc trong synchronized(DatabaseManager.getDbLock()).
 */
public class ItemDAO {

    private static final Logger logger = LoggerFactory.getLogger(ItemDAO.class);

    private Connection getConnection() {
        return DatabaseManager.getInstance().getConnection();
    }

    private Item mapRowToItem(ResultSet rs) throws SQLException {
        ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
        int id = rs.getInt("id");
        int sellerId = rs.getInt("seller_id");
        String name = rs.getString("name");
        String description = rs.getString("description");
        String imageUrl = rs.getString("image_url");
        double startingPrice = rs.getDouble("starting_price");

        Item item;
        switch (category) {
            case ELECTRONICS:
                Electronics elec = new Electronics();
                elec.setId(id);
                elec.setSellerId(sellerId);
                elec.setName(name);
                elec.setDescription(description);
                elec.setImageUrl(imageUrl);
                elec.setStartingPrice(startingPrice);
                elec.setCategory(category);
                parseElectronicsDetails(elec, description);
                item = elec;
                break;
            case ART:
                Art art = new Art();
                art.setId(id);
                art.setSellerId(sellerId);
                art.setName(name);
                art.setDescription(description);
                art.setImageUrl(imageUrl);
                art.setStartingPrice(startingPrice);
                art.setCategory(category);
                parseArtDetails(art, description);
                item = art;
                break;
            case VEHICLE:
                Vehicle vehicle = new Vehicle();
                vehicle.setId(id);
                vehicle.setSellerId(sellerId);
                vehicle.setName(name);
                vehicle.setDescription(description);
                vehicle.setImageUrl(imageUrl);
                vehicle.setStartingPrice(startingPrice);
                vehicle.setCategory(category);
                parseVehicleDetails(vehicle, description);
                item = vehicle;
                break;
            default:
                throw new SQLException("Unknown category: " + category);
        }

        item.setCreatedAt(DateTimeUtil.parseDb(rs.getString("created_at")));
        item.setUpdatedAt(DateTimeUtil.parseDb(rs.getString("updated_at")));
        return item;
    }

    private void parseElectronicsDetails(Electronics elec, String desc) {
        if (desc == null) return;
        String[] parts = desc.split("\\|");
        for (String part : parts) {
            if (part.startsWith("brand=")) elec.setBrand(part.substring(6));
            else if (part.startsWith("condition=")) elec.setCondition(part.substring(10));
            else if (part.startsWith("warranty=")) {
                try { elec.setWarrantyMonths(Integer.parseInt(part.substring(9))); }
                catch (NumberFormatException ignored) {}
            }
        }
    }

    private void parseArtDetails(Art art, String desc) {
        if (desc == null) return;
        String[] parts = desc.split("\\|");
        for (String part : parts) {
            if (part.startsWith("artist=")) art.setArtist(part.substring(7));
            else if (part.startsWith("year=")) {
                try { art.setYear(Integer.parseInt(part.substring(5))); }
                catch (NumberFormatException ignored) {}
            }
            else if (part.startsWith("medium=")) art.setMedium(part.substring(7));
        }
    }

    private void parseVehicleDetails(Vehicle v, String desc) {
        if (desc == null) return;
        String[] parts = desc.split("\\|");
        for (String part : parts) {
            if (part.startsWith("make=")) v.setMake(part.substring(5));
            else if (part.startsWith("model=")) v.setModelName(part.substring(6));
            else if (part.startsWith("yearMade=")) {
                try { v.setYearMade(Integer.parseInt(part.substring(9))); }
                catch (NumberFormatException ignored) {}
            }
            else if (part.startsWith("mileage=")) {
                try { v.setMileage(Integer.parseInt(part.substring(8))); }
                catch (NumberFormatException ignored) {}
            }
        }
    }

    private String buildDescription(Item item) {
        StringBuilder sb = new StringBuilder();
        if (item.getDescription() != null) {
            sb.append(item.getDescription());
        }

        if (item instanceof Electronics) {
            Electronics e = (Electronics) item;
            if (e.getBrand() != null) sb.append("|brand=").append(e.getBrand());
            if (e.getCondition() != null) sb.append("|condition=").append(e.getCondition());
            sb.append("|warranty=").append(e.getWarrantyMonths());
        } else if (item instanceof Art) {
            Art a = (Art) item;
            if (a.getArtist() != null) sb.append("|artist=").append(a.getArtist());
            sb.append("|year=").append(a.getYear());
            if (a.getMedium() != null) sb.append("|medium=").append(a.getMedium());
        } else if (item instanceof Vehicle) {
            Vehicle v = (Vehicle) item;
            if (v.getMake() != null) sb.append("|make=").append(v.getMake());
            if (v.getModelName() != null) sb.append("|model=").append(v.getModelName());
            sb.append("|yearMade=").append(v.getYearMade());
            sb.append("|mileage=").append(v.getMileage());
        }

        return sb.toString();
    }

    public Item insert(Item item) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "INSERT INTO items (seller_id, name, description, category, image_url, starting_price) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, item.getSellerId());
                ps.setString(2, item.getName());
                ps.setString(3, buildDescription(item));
                ps.setString(4, item.getCategory().name());
                ps.setString(5, item.getImageUrl());
                ps.setDouble(6, item.getStartingPrice());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        item.setId(keys.getInt(1));
                    }
                }
            }
            logger.info("Inserted item: {} ({})", item.getName(), item.getCategory());
            return item;
        }
    }

    public Optional<Item> findById(int id) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM items WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRowToItem(rs));
                    }
                }
            }
            return Optional.empty();
        }
    }

    public List<Item> findBySellerId(int sellerId) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM items WHERE seller_id = ? ORDER BY created_at DESC";
            List<Item> items = new ArrayList<>();

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, sellerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRowToItem(rs));
                    }
                }
            }
            return items;
        }
    }

    public List<Item> searchByName(String keyword) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM items WHERE name LIKE ? ORDER BY created_at DESC";
            List<Item> items = new ArrayList<>();

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, "%" + keyword + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRowToItem(rs));
                    }
                }
            }
            return items;
        }
    }

    public List<Item> findByCategory(ItemCategory category) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "SELECT * FROM items WHERE category = ? ORDER BY created_at DESC";
            List<Item> items = new ArrayList<>();

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, category.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(mapRowToItem(rs));
                    }
                }
            }
            return items;
        }
    }

    public void update(Item item) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "UPDATE items SET name = ?, description = ?, category = ?, " +
                         "image_url = ?, starting_price = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setString(1, item.getName());
                ps.setString(2, buildDescription(item));
                ps.setString(3, item.getCategory().name());
                ps.setString(4, item.getImageUrl());
                ps.setDouble(5, item.getStartingPrice());
                ps.setInt(6, item.getId());
                ps.executeUpdate();
            }
            logger.info("Updated item: {}", item.getName());
        }
    }

    public void delete(int id) throws SQLException {
        synchronized (DatabaseManager.getDbLock()) {
            String sql = "DELETE FROM items WHERE id = ?";

            try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            logger.info("Deleted item: {}", id);
        }
    }
}
