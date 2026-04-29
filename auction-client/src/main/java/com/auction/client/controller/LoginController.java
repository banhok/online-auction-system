package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * LoginController — điều khiển màn hình đăng nhập.
 */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private Button btnRegister;
    @FXML private Label lblStatus;
    @FXML private ProgressIndicator progressIndicator;

    @FXML
    public void initialize() {
        if (progressIndicator != null) {
            progressIndicator.setVisible(false);
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập đầy đủ thông tin");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                ServerConnection conn = ServerConnection.getInstance();

                // Kết nối nếu chưa
                if (!conn.isConnected()) {
                    conn.connect("localhost", 8888);
                }

                // Gửi request đăng nhập
                Request req = new Request(CommandType.LOGIN);
                req.put("username", username);
                req.put("password", password);
                Response response = conn.sendRequest(req);

                Platform.runLater(() -> {
                    setLoading(false);
                    if (response.isSuccess()) {
                        // Lưu thông tin user
                        conn.setCurrentUserId(response.getInt("userId"));
                        conn.setCurrentUsername(response.getString("username"));
                        conn.setCurrentRole(response.getString("role"));
                        conn.setCurrentBalance(response.getDouble("balance"));

                        // Chuyển tới dashboard
                        SceneManager.getInstance().switchScene(
                                "dashboard.fxml", "Dashboard", 1200, 800);
                    } else {
                        AlertUtil.showError("Đăng nhập thất bại", response.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    AlertUtil.showError("Lỗi kết nối",
                            "Không thể kết nối tới server.\n" + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleGoToRegister() {
        SceneManager.getInstance().switchScene("register.fxml", "Đăng ký", 500, 600);
    }

    private void setLoading(boolean loading) {
        btnLogin.setDisable(loading);
        btnRegister.setDisable(loading);
        if (progressIndicator != null) {
            progressIndicator.setVisible(loading);
        }
        if (lblStatus != null) {
            lblStatus.setText(loading ? "Đang đăng nhập..." : "");
        }
    }
}
