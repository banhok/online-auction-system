package com.auction.common.model;

/**
 * Lớp trừu tượng Item — sản phẩm đấu giá.
 * Kế thừa Entity. Các lớp con: Electronics, Art, Vehicle.
 * Inheritance: Entity -> Item -> Electronics/Art/Vehicle
 */
public abstract class Item extends Entity {

    private static final long serialVersionUID = 1L;

    private int sellerId;
    private String name;
    private String description;
    private ItemCategory category;
    private String imageUrl;
    private double startingPrice;

    protected Item() {
        super();
    }

    protected Item(int id, int sellerId, String name, String description,
                   ItemCategory category, String imageUrl, double startingPrice) {
        super(id);
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.startingPrice = startingPrice;
    }

    protected Item(int sellerId, String name, String description,
                   ItemCategory category, String imageUrl, double startingPrice) {
        super();
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.startingPrice = startingPrice;
    }

    // --- Getter / Setter ---

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    /**
     * Phương thức trừu tượng — mỗi loại sản phẩm có thuộc tính riêng.
     * Thể hiện Polymorphism.
     */
    public abstract String getCategoryDetails();

    @Override
    public String printInfo() {
        return String.format("[%s] %s - Starting: %.2f - %s",
                category, name, startingPrice, getCategoryDetails());
    }
}
