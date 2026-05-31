package com.auction.client;

import com.auction.client.network.ServerConnection;
import com.auction.client.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * ClientApp — Entry point của Client JavaFX.
 * Mở màn hình đăng nhập, quản lý vòng đời ứng dụng.
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.getInstance().setPrimaryStage(primaryStage);

        // Cấu hình cửa sổ
        primaryStage.setTitle("Auction System - Đăng nhập");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(400);

        // Khi đóng cửa sổ → ngắt kết nối + thoát
        primaryStage.setOnCloseRequest(event -> {
            ServerConnection.getInstance().disconnect();
            Platform.exit();
            System.exit(0);
        });

        // Mở màn hình đăng nhập
        SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập", 500, 500);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
