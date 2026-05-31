package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.User;
import com.auction.common.util.JsonUtil;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.AvatarInitials;
import com.auction.client.util.ModalDialog;
import com.auction.client.util.MoneyFormatter;
import com.auction.client.util.SceneManager;
import com.auction.client.util.ToastUtil;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * AdminPanelController — quản trị hệ thống (Admin only).
 * Xem danh sách user, xóa user.
 */
public class AdminPanelController {

    @FXML private TableView<User> tableUsers;
    @FXML private TableColumn<User, String> colUserId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colFullName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, String> colBalance;
    @FXML private Label lblTotalUsers;
    @FXML private Button btnRefresh;
    @FXML private Button btnDeleteUser;
    @FXML private Button btnBack;
    @FXML private StackPane avatarSlot;

    private final ServerConnection conn = ServerConnection.getInstance();
    private final ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (avatarSlot != null) {
            avatarSlot.getChildren().setAll(AvatarInitials.create(conn.getCurrentUsername(), 36));
        }
        colUserId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getId())));
        colUsername.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colFullName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole().name()));
        colBalance.setCellValueFactory(d -> new SimpleStringProperty(
                MoneyFormatter.format(d.getValue().getBalance())));

        tableUsers.setItems(userList);
        loadUsers();
    }

    @FXML
    private void loadUsers() {
        new Thread(() -> {
            try {
                Response resp = conn.send(CommandType.GET_ALL_USERS);
                if (resp.isSuccess()) {
                    List<User> users = JsonUtil.getGson().fromJson(
                            resp.getString("users"),
                            new TypeToken<List<User>>() {}.getType());
                    Platform.runLater(() -> {
                        userList.clear();
                        userList.addAll(users);
                        lblTotalUsers.setText("Tổng: " + users.size() + " người dùng");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDeleteUser() {
        User selected = tableUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn user cần xóa");
            return;
        }
        if (selected.getId() == conn.getCurrentUserId()) {
            AlertUtil.showError("Lỗi", "Không thể xóa chính mình");
            return;
        }
        if (!ModalDialog.confirm(btnDeleteUser.getScene().getWindow(),
                "Xác nhận", "Xóa user " + selected.getUsername() + "?")) {
            return;
        }

        new Thread(() -> {
            try {
                Request req = new Request(CommandType.DELETE_USER);
                req.put("targetUserId", selected.getId());
                Response resp = conn.sendRequest(req);
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        ToastUtil.success(lblTotalUsers, "Đã xóa user " + selected.getUsername());
                        loadUsers();
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchScene("dashboard.fxml", "Dashboard",
                SceneManager.MAIN_WIDTH, SceneManager.MAIN_HEIGHT);
    }
}
