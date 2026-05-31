package com.auction.client.util;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Tạo avatar tròn từ chữ cái đầu username, màu hash từ username.
 * Dùng cho top bar + cột bidder trong table.
 *
 * Pattern: <code>topBar.getChildren().add(AvatarInitials.create("alice", 36));</code>
 */
public final class AvatarInitials {

    /** 10 màu pastel — chia theo hashCode(username) để mỗi user có 1 màu ổn định. */
    private static final String[] COLORS = {
            "#16a34a", "#0ea5e9", "#f59e0b", "#ef4444", "#a855f7",
            "#ec4899", "#14b8a6", "#f97316", "#8b5cf6", "#06b6d4"
    };

    private AvatarInitials() {}

    public static StackPane create(String name, double size) {
        Circle bg = new Circle(size / 2);
        bg.setFill(Color.web(colorFor(name)));

        Label letter = new Label(initialOf(name));
        letter.getStyleClass().add("avatar-label");
        letter.setStyle("-fx-font-size: " + (size * 0.42) + "px;");

        StackPane pane = new StackPane(bg, letter);
        pane.getStyleClass().add("avatar-circle");
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        return pane;
    }

    private static String colorFor(String name) {
        if (name == null || name.isEmpty()) return COLORS[0];
        return COLORS[Math.abs(name.hashCode()) % COLORS.length];
    }

    private static String initialOf(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }
}
