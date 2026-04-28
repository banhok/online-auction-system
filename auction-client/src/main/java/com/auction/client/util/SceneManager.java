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
     * @param fxmlFile tên file FXML (ví dụ: "login.fxml")
     * @param title tiêu đề cửa sổ
     */
    public void switchScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/" + fxmlFile));
            Parent root = loader.load();

            // Lưu controller để truy cập sau
            Object controller = loader.getController();
            if (controller != null) {
                controllers.put(fxmlFile, controller);
            }

            Scene scene = new Scene(root);

            // Load CSS
            String css = getClass().getResource("/css/style.css") != null
                    ? getClass().getResource("/css/style.css").toExternalForm()
                    : null;
            if (css != null) {
                scene.getStylesheets().add(css);
            }

            primaryStage.setTitle("Auction System - " + title);
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Switched to scene: {}", fxmlFile);
        } catch (IOException e) {
            logger.error("Failed to load scene: {}", fxmlFile, e);
            AlertUtil.showError("Lỗi", "Không thể tải màn hình: " + fxmlFile);
        }
    }

    /**
     * Chuyển scene với kích thước cụ thể.
     */
    public void switchScene(String fxmlFile, String title, double width, double height) {
        switchScene(fxmlFile, title);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
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
