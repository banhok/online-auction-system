package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.WalletTransaction;
import com.auction.common.util.DateTimeUtil;
import com.auction.common.util.JsonUtil;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.AvatarInitials;
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
import java.util.function.Consumer;

/**
 * ProfileController — trang cá nhân.
 * Xem/sửa thông tin, đổi mật khẩu, xem lịch sử giao dịch ví.
 */
public class ProfileController {

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtOldPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmNewPassword;

    @FXML private TableView<WalletTransaction> tableWalletHistory;
    @FXML private TableColumn<WalletTransaction, String> colTxType;
    @FXML private TableColumn<WalletTransaction, String> colTxAmount;
    @FXML private TableColumn<WalletTransaction, String> colTxDesc;
    @FXML private TableColumn<WalletTransaction, String> colTxTime;

    @FXML private Button btnBack;
    @FXML private StackPane avatarSlot;
    @FXML private Label lblStatDeposit;
    @FXML private Label lblStatWins;
    @FXML private Label lblStatHold;
    @FXML private Label lblStatTxCount;

    private final ServerConnection conn = ServerConnection.getInstance();
    private final ObservableList<WalletTransaction> txList = FXCollections.observableArrayList();
    private Consumer<Response> realtimeListener;

    @FXML
    public void initialize() {
        lblUsername.setText(conn.getCurrentUsername());
        lblRole.setText(conn.getCurrentRole());
        lblBalance.setText(MoneyFormatter.format(conn.getCurrentBalance()));
        if (avatarSlot != null) {
            avatarSlot.getChildren().setAll(AvatarInitials.create(conn.getCurrentUsername(), 36));
        }

        // Table setup
        colTxType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getType().name()));
        colTxAmount.setCellValueFactory(d -> new SimpleStringProperty(
                MoneyFormatter.format(d.getValue().getAmount())));
        colTxDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colTxTime.setCellValueFactory(d -> new SimpleStringProperty(
                DateTimeUtil.formatDisplay(d.getValue().getCreatedAt())));
        tableWalletHistory.setItems(txList);

        // Load data
        loadProfile();
        loadWalletHistory();

        // Lắng nghe BALANCE_UPDATE để label số dư + lịch sử giao dịch tự refresh
        realtimeListener = this::handleRealtimeEvent;
        conn.addEventListener(realtimeListener);
    }

    /**
     * Cập nhật label số dư + reload lịch sử giao dịch khi balance thay đổi.
     */
    private void handleRealtimeEvent(Response response) {
        if (response.getCommand() == CommandType.BALANCE_UPDATE) {
            double newBalance = response.getDouble("balance");
            conn.setCurrentBalance(newBalance);
            Platform.runLater(() -> {
                lblBalance.setText(MoneyFormatter.format(newBalance));
                loadWalletHistory();
            });
        }
    }

    private void loadProfile() {
        new Thread(() -> {
            try {
                Response resp = conn.send(CommandType.GET_PROFILE);
                if (resp.isSuccess()) {
                    Platform.runLater(() -> {
                        txtFullName.setText(resp.getString("fullName"));
                        txtEmail.setText(resp.getString("email"));
                        conn.setCurrentBalance(resp.getDouble("balance"));
                        lblBalance.setText(MoneyFormatter.format(resp.getDouble("balance")));
                    });
                }
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }

    @FXML
    private void handleUpdateProfile() {
        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();

        if (fullName.isEmpty() || email.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập đầy đủ thông tin");
            return;
        }

        new Thread(() -> {
            try {
                Request req = new Request(CommandType.UPDATE_PROFILE);
                req.put("fullName", fullName);
                req.put("email", email);
                Response resp = conn.sendRequest(req);
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        ToastUtil.success(lblBalance, "Cập nhật thông tin thành công");
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
    private void handleChangePassword() {
        String oldPw = txtOldPassword.getText();
        String newPw = txtNewPassword.getText();
        String confirmPw = txtConfirmNewPassword.getText();

        if (oldPw.isEmpty() || newPw.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập đầy đủ");
            return;
        }
        if (!newPw.equals(confirmPw)) {
            AlertUtil.showWarning("Cảnh báo", "Mật khẩu mới không khớp");
            return;
        }

        new Thread(() -> {
            try {
                Request req = new Request(CommandType.CHANGE_PASSWORD);
                req.put("oldPassword", oldPw);
                req.put("newPassword", newPw);
                Response resp = conn.sendRequest(req);
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        ToastUtil.success(lblBalance, "Đổi mật khẩu thành công");
                        txtOldPassword.clear();
                        txtNewPassword.clear();
                        txtConfirmNewPassword.clear();
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    private void loadWalletHistory() {
        new Thread(() -> {
            try {
                Response resp = conn.send(CommandType.GET_WALLET_HISTORY);
                if (resp.isSuccess()) {
                    List<WalletTransaction> txs = JsonUtil.getGson().fromJson(
                            resp.getString("transactions"),
                            new TypeToken<List<WalletTransaction>>() {}.getType());
                    Platform.runLater(() -> {
                        txList.clear();
                        txList.addAll(txs);
                        updateStats(txs);
                    });
                }
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }

    /**
     * Tính 4 stats từ wallet history client-side.
     * Tổng nạp (DEPOSIT typical = nạp), Tổng thắng (mô tả chứa "thanh toán"/"Nhận"),
     * Cọc đang giữ (BID_HOLD - BID_REFUND), Số giao dịch.
     */
    private void updateStats(List<WalletTransaction> txs) {
        double totalDeposit = 0, totalWins = 0, totalHold = 0;
        for (WalletTransaction tx : txs) {
            if (tx.getType() == null) continue;
            switch (tx.getType()) {
                case DEPOSIT:
                    totalDeposit += tx.getAmount();
                    // Phân biệt seller nhận thanh toán (DEPOSIT + description "Nhận thanh toán")
                    if (tx.getDescription() != null && tx.getDescription().toLowerCase().contains("nhận")) {
                        totalWins += tx.getAmount();
                        totalDeposit -= tx.getAmount();
                    }
                    break;
                case BID_HOLD:    totalHold += tx.getAmount(); break;
                case BID_REFUND:  totalHold -= tx.getAmount(); break;
                case PAYMENT:     totalWins += tx.getAmount(); break;
                default: break;
            }
        }
        if (totalHold < 0) totalHold = 0;
        if (lblStatDeposit != null) lblStatDeposit.setText(MoneyFormatter.format(totalDeposit));
        if (lblStatWins != null) lblStatWins.setText(MoneyFormatter.format(totalWins));
        if (lblStatHold != null) lblStatHold.setText(MoneyFormatter.format(totalHold));
        if (lblStatTxCount != null) lblStatTxCount.setText(String.valueOf(txs.size()));
    }

    @FXML
    private void handleBack() {
        if (realtimeListener != null) conn.removeEventListener(realtimeListener);
        SceneManager.getInstance().switchScene("dashboard.fxml", "Dashboard",
                SceneManager.MAIN_WIDTH, SceneManager.MAIN_HEIGHT);
    }
}
