package com.auction.client.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;

/**
 * Overlay mờ + spinner — dùng quanh các sendRequest đắt (bid/join/pay/create).
 * Dùng Popup phủ toàn bộ owner window (không cần FXML root StackPane).
 *
 * Pattern dùng:
 * <pre>
 *   LoadingOverlay overlay = LoadingOverlay.show(anyNodeInScene);
 *   new Thread(() -> {
 *       try { ... } finally { Platform.runLater(overlay::hide); }
 *   }).start();
 * </pre>
 */
public final class LoadingOverlay {

    private Popup popup;
    private final Node anchor;

    private LoadingOverlay(Node anchor) {
        this.anchor = anchor;
    }

    public static LoadingOverlay show(Node anchor) {
        LoadingOverlay overlay = new LoadingOverlay(anchor);
        overlay.doShow();
        return overlay;
    }

    private void doShow() {
        Platform.runLater(() -> {
            if (anchor == null || anchor.getScene() == null) return;
            Window owner = anchor.getScene().getWindow();
            if (owner == null) return;

            ProgressIndicator pi = new ProgressIndicator();
            pi.getStyleClass().add("loading-spinner");
            pi.setPrefSize(80, 80);

            StackPane root = new StackPane(pi);
            root.getStyleClass().add("loading-overlay");
            root.setPrefSize(owner.getWidth(), owner.getHeight());

            popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(false);
            popup.show(owner, owner.getX(), owner.getY());
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            if (popup != null) {
                popup.hide();
                popup = null;
            }
        });
    }
}
