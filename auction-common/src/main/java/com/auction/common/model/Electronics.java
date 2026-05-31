package com.auction.common.model;

/**
 * Electronics — sản phẩm điện tử.
 * Kế thừa Item, category = ELECTRONICS.
 * Thuộc tính riêng: thương hiệu, tình trạng (mới/cũ), bảo hành.
 */
public class Electronics extends Item {

    private static final long serialVersionUID = 1L;

    private String brand;
    private String condition; // "NEW", "USED", "REFURBISHED"
    private int warrantyMonths;

    public Electronics() {
        super();
        setCategory(ItemCategory.ELECTRONICS);
    }

    public Electronics(int id, int sellerId, String name, String description,
                       String imageUrl, double startingPrice,
                       String brand, String condition, int warrantyMonths) {
        super(id, sellerId, name, description, ItemCategory.ELECTRONICS, imageUrl, startingPrice);
        this.brand = brand;
        this.condition = condition;
        this.warrantyMonths = warrantyMonths;
    }

    public Electronics(int sellerId, String name, String description,
                       String imageUrl, double startingPrice,
                       String brand, String condition, int warrantyMonths) {
        super(sellerId, name, description, ItemCategory.ELECTRONICS, imageUrl, startingPrice);
        this.brand = brand;
        this.condition = condition;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getCategoryDetails() {
        return String.format("Brand: %s, Condition: %s, Warranty: %d months",
                brand, condition, warrantyMonths);
    }
}
