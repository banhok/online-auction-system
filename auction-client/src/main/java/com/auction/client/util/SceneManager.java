package com.auction.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SceneManager — quản lý chuyển đổi màn hình JavaFX.
 * Singleton, load FXML và chuyển scene.
 */
public class SceneManager {

    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static SceneManager instance;

    private Stage primaryStage;
    private final Map<String, Object> controllers = new HashMap<>();

    private SceneManager() {
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Chuyển sang màn hình khác.
     *
     * Lần đầu: tạo Scene mới + gắn CSS + setScene(). Các lần sau dùng `scene.setRoot(newRoot)`
     * — giữ nguyên Scene đang gắn vào Stage → không re-create native window → switch mượt mà
     * và giữ trạng thái cửa sổ (maximize/fullscreen/position) qua các lần chuyển.
     *
     * @param fxmlFile tên file FXML (ví dụ: "login.fxml")
     * @param title tiêu đề cửa sổ
     */
    public void switchScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller != null) {
                controllers.put(fxmlFile, controller);
            }

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                // Lần đầu — tạo Scene + gắn CSS
                scene = new Scene(root);
                String css = getClass().getResource("/css/style.css") != null
                        ? getClass().getResource("/css/style.css").toExternalForm()
                        : null;
                if (css != null) {
                    scene.getStylesheets().add(css);
                }
                primaryStage.setScene(scene);
            } else {
                // Các lần sau — chỉ swap root, giữ Scene + CSS + Stage state
                scene.setRoot(root);
            }

            primaryStage.setTitle("Auction System - " + title);
            primaryStage.show();

            logger.info("Switched to scene: {}", fxmlFile);
        } catch (IOException e) {
            logger.error("Failed to load scene: {}", fxmlFile, e);
            AlertUtil.showError("Lỗi", "Không thể tải màn hình: " + fxmlFile);
        }
    }

    /** Kích thước chuẩn cho các main screen sau login — dùng cùng size để switch mượt. */
    public static final double MAIN_WIDTH = 1300;
    public static final double MAIN_HEIGHT = 820;
    /** Min size — user không thu nhỏ window được dưới mức này (tránh cut layout). */
    public static final double MIN_WIDTH = 1100;
    public static final double MIN_HEIGHT = 720;

    /**
     * Chuyển scene với kích thước cụ thể.
     *
     * Rule:
     * - Nếu Stage đang maximize / fullscreen / iconified → KHÔNG động kích thước
     *   (user đang ở chế độ phóng to, switch scene phải giữ nguyên).
     * - Còn lại, chỉ resize + center nếu kích thước HIỆN TẠI khác w/h → switch giữa các
     *   main screen cùng size không bị "khựng" do window resize/reposition.
     */
    public void switchScene(String fxmlFile, String title, double width, double height) {
        boolean preserveState = primaryStage != null
                && (primaryStage.isMaximized() || primaryStage.isFullScreen()
                    || primaryStage.isIconified());

        switchScene(fxmlFile, title);

        // Set min size cho main screen — tránh user thu nhỏ window làm layout vỡ.
        // Login/Register có size nhỏ riêng → không apply min size đó.
        if (width >= MAIN_WIDTH) {
            primaryStage.setMinWidth(MIN_WIDTH);
            primaryStage.setMinHeight(MIN_HEIGHT);
        } else {
            // Login/Register — bỏ min size
            primaryStage.setMinWidth(0);
            primaryStage.setMinHeight(0);
        }

        if (preserveState) return;

        boolean needResize = Math.abs(primaryStage.getWidth() - width) > 1
                || Math.abs(primaryStage.getHeight() - height) > 1;
        if (needResize) {
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();
        }
    }

    /**
     * Lấy controller của scene hiện tại hoặc đã load.
     */
    @SuppressWarnings("unchecked")
    public <T> T getController(String fxmlFile) {
        return (T) controllers.get(fxmlFile);
    }

    /**
     * Load FXML và trả về root node (dùng cho popup, dialog).
     */
    public FXMLLoader loadFXML(String fxmlFile) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/" + fxmlFile));
        loader.load();
        return loader;
    }
}
