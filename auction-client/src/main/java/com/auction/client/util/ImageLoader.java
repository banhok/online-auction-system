package com.auction.client.util;

import javafx.scene.image.Image;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Load Image từ URL với placeholder fallback + cache.
 * Dùng cho item.imageUrl ở dashboard / auction_detail / seller_panel.
 *
 * Pattern:
 * <pre>
 *   imageView.setImage(ImageLoader.load(item.getImageUrl(), 200, 200));
 * </pre>
 */
public final class ImageLoader {

    /** Placeholder hiển thị khi URL null/blank/lỗi. */
    private static final Image PLACEHOLDER;

    static {
        java.net.URL res = ImageLoader.class.getResource("/images/placeholder.png");
        PLACEHOLDER = (res != null) ? new Image(res.toExternalForm()) : null;
    }

    /** Cache Image theo URL — tránh load lại cùng URL nhiều lần. */
    private static final Map<String, Image> cache = new ConcurrentHashMap<>();

    private ImageLoader() {}

    /**
     * Load ảnh từ URL với requested size. Backgroud loading async — UI không freeze.
     * Khi load lỗi (404, timeout, không phải image) → cache placeholder để khỏi retry.
     */
    public static Image load(String url, double requestedWidth, double requestedHeight) {
        if (url == null || url.isBlank()) return PLACEHOLDER;

        Image cached = cache.get(url);
        if (cached != null) return cached;

        Image img = new Image(url, requestedWidth, requestedHeight, true, true, true);
        img.errorProperty().addListener((obs, oldVal, isError) -> {
            if (isError && PLACEHOLDER != null) {
                cache.put(url, PLACEHOLDER);
            }
        });
        cache.put(url, img);
        return img;
    }

    /** Load không scale (giữ kích thước gốc). */
    public static Image load(String url) {
        return load(url, 0, 0);
    }

    public static Image placeholder() {
        return PLACEHOLDER;
    }
}
