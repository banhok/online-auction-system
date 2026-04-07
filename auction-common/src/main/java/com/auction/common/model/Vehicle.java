package com.auction.common.model;

/**
 * Vehicle — phương tiện giao thông.
 * Kế thừa Item, category = VEHICLE.
 * Thuộc tính riêng: hãng xe, năm sản xuất, số km đã đi.
 */
public class Vehicle extends Item {

    private static final long serialVersionUID = 1L;

    private String make;       // Hãng xe: Toyota, Honda...
    private String modelName;  // Tên mẫu: Camry, Civic...
    private int yearMade;
    private int mileage;       // Số km đã đi

    public Vehicle() {
        super();
        setCategory(ItemCategory.VEHICLE);
    }

    public Vehicle(int id, int sellerId, String name, String description,
                   String imageUrl, double startingPrice,
                   String make, String modelName, int yearMade, int mileage) {
        super(id, sellerId, name, description, ItemCategory.VEHICLE, imageUrl, startingPrice);
        this.make = make;
        this.modelName = modelName;
        this.yearMade = yearMade;
        this.mileage = mileage;
    }

    public Vehicle(int sellerId, String name, String description,
                   String imageUrl, double startingPrice,
                   String make, String modelName, int yearMade, int mileage) {
        super(sellerId, name, description, ItemCategory.VEHICLE, imageUrl, startingPrice);
        this.make = make;
        this.modelName = modelName;
        this.yearMade = yearMade;
        this.mileage = mileage;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getYearMade() {
        return yearMade;
    }

    public void setYearMade(int yearMade) {
        this.yearMade = yearMade;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    @Override
    public String getCategoryDetails() {
        return String.format("Make: %s %s, Year: %d, Mileage: %dkm",
                make, modelName, yearMade, mileage);
    }
}
