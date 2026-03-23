package com.onlineauction.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label titleLabel = new Label("HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label subtitleLabel = new Label("Login");
        subtitleLabel.setStyle("-fx-font-size: 14px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");

        Button loginButton = new Button("Login");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: red;");

        loginButton.setOnAction(event -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                messageLabel.setText("Please enter both username and password.");
            } else {
                messageLabel.setStyle("-fx-text-fill: green;");
                messageLabel.setText("Login UI works. Ready to connect server later.");
            }
        });

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(380);

        root.getChildren().addAll(
                titleLabel,
                subtitleLabel,
                usernameField,
                passwordField,
                loginButton,
                messageLabel
        );

        Scene scene = new Scene(root, 420, 300);

        primaryStage.setTitle("Online Auction - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
