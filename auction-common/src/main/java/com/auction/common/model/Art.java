package com.auction.common.model;

/**
 * Art — tác phẩm nghệ thuật.
 * Kế thừa Item, category = ART.
 * Thuộc tính riêng: tác giả, năm sáng tác, chất liệu.
 */
public class Art extends Item {

    private static final long serialVersionUID = 1L;

    private String artist;
    private int year;
    private String medium; // "Oil", "Watercolor", "Digital", "Sculpture"...

    public Art() {
        super();
        setCategory(ItemCategory.ART);
    }

    public Art(int id, int sellerId, String name, String description,
               String imageUrl, double startingPrice,
               String artist, int year, String medium) {
        super(id, sellerId, name, description, ItemCategory.ART, imageUrl, startingPrice);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    public Art(int sellerId, String name, String description,
               String imageUrl, double startingPrice,
               String artist, int year, String medium) {
        super(sellerId, name, description, ItemCategory.ART, imageUrl, startingPrice);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getMedium() {
        return medium;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    @Override
    public String getCategoryDetails() {
        return String.format("Artist: %s, Year: %d, Medium: %s",
                artist, year, medium);
    }
}
