package com.auction.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp trừu tượng gốc cho tất cả entity trong hệ thống.
 * Áp dụng: Abstraction + Encapsulation.
 * Mọi entity đều có id, thời gian tạo và cập nhật.
 */
public abstract class Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor mặc định
    protected Entity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor có id
    protected Entity(int id) {
        this.id = id;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getter / Setter (Encapsulation) ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Phương thức trừu tượng — mỗi lớp con tự định nghĩa cách hiển thị.
     * Thể hiện Polymorphism: cùng gọi printInfo() nhưng kết quả khác nhau.
     */
    public abstract String printInfo();

    @Override
    public String toString() {
        return printInfo();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
