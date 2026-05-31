package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.SceneManager;
import com.auction.client.util.ToastUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * RegisterController — điều khiển màn hình đăng ký.
 */
public class RegisterController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cboRole;
    @FXML private Button btnRegister;
    @FXML private Button btnBack;
    @FXML private ProgressIndicator progressIndicator;

    @FXML
    public void initialize() {
        cboRole.getItems().addAll("BIDDER", "SELLER");
        cboRole.setValue("BIDDER");
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String role = cboRole.getValue();

        // Validate
        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if (!password.equals(confirmPassword)) {
            AlertUtil.showWarning("Cảnh báo", "Mật khẩu xác nhận không khớp");
            return;
        }
        if (password.length() < 6) {
            AlertUtil.showWarning("Cảnh báo", "Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();
                if (!conn.isConnected()) {
                    conn.connect("localhost", 8888);
                }

                Request req = new Request(CommandType.REGISTER);
                req.put("username", username);
                req.put("password", password);
                req.put("fullName", fullName);
                req.put("email", email);
                req.put("role", role);
                Response response = conn.sendRequest(req);

                Platform.runLater(() -> {
                    setLoading(false);
                    if (response.isSuccess()) {
                        ToastUtil.success(btnRegister, "Đăng ký thành công! Hãy đăng nhập.");
                        handleBack();
                    } else {
                        AlertUtil.showError("Đăng ký thất bại", response.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    AlertUtil.showError("Lỗi", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập", 500, 500);
    }

    private void setLoading(boolean loading) {
        btnRegister.setDisable(loading);
        btnBack.setDisable(loading);
        if (progressIndicator != null) {
            progressIndicator.setVisible(loading);
        }
    }
}
