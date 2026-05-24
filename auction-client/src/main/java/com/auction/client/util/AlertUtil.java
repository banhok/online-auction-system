package com.auction.client.util;

import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/**
 * AlertUtil — tiện ích popup. Hiện chuyển toàn bộ sang ModalDialog (custom UI đồng nhất theme),
 * không còn dùng JavaFX Alert mặc định Windows.
 *
 * Owner Window mặc định lấy từ {@link SceneManager#getPrimaryStage()} — gọi từ bất kỳ controller nào
 * đều hoạt động đúng.
 */
public final class AlertUtil {

    private AlertUtil() {}

    private static Window owner() {
        try {
            Stage stage = SceneManager.getInstance().getPrimaryStage();
            return stage;
        } catch (Exception e) {
            return null;
        }
    }

    public static void showInfo(String title, String message) {
        ModalDialog.info(owner(), title, message);
    }

    public static void showError(String title, String message) {
        ModalDialog.error(owner(), title, message);
    }

    public static void showWarning(String title, String message) {
        ModalDialog.warning(owner(), title, message);
    }

    public static boolean showConfirm(String title, String message) {
        return ModalDialog.confirm(owner(), title, message);
    }

    public static Optional<String> showInput(String title, String message, String defaultValue) {
        return ModalDialog.input(owner(), title, message, defaultValue);
    }
}
