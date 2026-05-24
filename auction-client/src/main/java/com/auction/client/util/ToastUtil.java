package com.auction.client.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Toast notification — popup non-blocking, slide-in từ góc dưới phải, auto-dismiss 3s.
 * Dùng cho event realtime non-critical (BID_UPDATE, AUCTION_EXTENDED, BALANCE_UPDATE).
 * Lỗi critical / cần user xác nhận → vẫn dùng AlertUtil.
 */
public final class ToastUtil {

    private static final double DISPLAY_MS = 3000;
    private static final double FADE_MS = 300;
    private static final double TOAST_WIDTH = 360;
    private static final double TOAST_HEIGHT = 50;
    private static final double MARGIN = 20;

    /** Stack các toast đang hiển thị để offset Y không đè nhau. */
    private static final List<Popup> active = new ArrayList<>();

    private ToastUtil() {}

    public static void info(Node anchor, String message)    { show(anchor, message, "toast-info"); }
    public static void success(Node anchor, String message) { show(anchor, message, "toast-success"); }
    public static void warning(Node anchor, String message) { show(anchor, message, "toast-warning"); }
    public static void danger(Node anchor, String message)  { show(anchor, message, "toast-danger"); }

    private static void show(Node anchor, String message, String cssClass) {
        Platform.runLater(() -> {
            if (anchor == null || anchor.getScene() == null) return;
            Window owner = anchor.getScene().getWindow();
            if (owner == null) return;

            Label lbl = new Label(message);
            lbl.getStyleClass().addAll("toast-base", cssClass);
            lbl.setWrapText(true);
            lbl.setMaxWidth(TOAST_WIDTH);
            lbl.setPrefHeight(TOAST_HEIGHT);
            lbl.setMinHeight(TOAST_HEIGHT);

            StackPane wrap = new StackPane(lbl);
            wrap.setStyle("-fx-background-color: transparent;");

            Popup popup = new Popup();
            popup.getContent().add(wrap);
            popup.setAutoHide(false);

            int index = active.size();
            double x = owner.getX() + owner.getWidth() - TOAST_WIDTH - MARGIN;
            double y = owner.getY() + owner.getHeight()
                    - (TOAST_HEIGHT + 10) * (index + 1) - MARGIN;
            popup.show(owner, x, y);
            active.add(popup);

            wrap.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(FADE_MS), wrap);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            PauseTransition hold = new PauseTransition(Duration.millis(DISPLAY_MS));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(FADE_MS), wrap);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            SequentialTransition seq = new SequentialTransition(fadeIn, hold, fadeOut);
            seq.setOnFinished(e -> {
                popup.hide();
                active.remove(popup);
            });
            seq.play();
        });
    }
}
