package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.Auction;
//import com.auction.common.model.AuctionStatus;
import com.auction.common.util.DateTimeUtil;
import com.auction.common.util.JsonUtil;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.MoneyFormatter;
import com.auction.client.util.SceneManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * DashboardController — màn hình chính sau đăng nhập.
 * Hiển thị danh sách phiên đấu giá, tìm kiếm, lọc.
 */
public class DashboardController {

    @FXML private Label lblWelcome;
    @FXML private Label lblBalance;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cboStatusFilter;
    @FXML private TableView<Auction> tableAuctions;
    @FXML private TableColumn<Auction, String> colItem;
    @FXML private TableColumn<Auction, String> colPrice;
    @FXML private TableColumn<Auction, String> colBids;
    @FXML private TableColumn<Auction, String> colStatus;
    @FXML private TableColumn<Auction, String> colStartTime;
    @FXML private TableColumn<Auction, String> colEndTime;
    @FXML private Button btnRefresh;
    @FXML private Button btnSellerPanel;
    @FXML private Button btnAdminPanel;
    @FXML private Button btnProfile;
    @FXML private Button btnTopUp;
    @FXML private Button btnLogout;

    // Tab "Phiên đã thắng"
    @FXML private TableView<Auction> tableMyWins;
    @FXML private TableColumn<Auction, String> colWinItem;
    @FXML private TableColumn<Auction, String> colWinPrice;
    @FXML private TableColumn<Auction, String> colWinStatus;
    @FXML private TableColumn<Auction, String> colWinEndTime;
    @FXML private Button btnRefreshWins;

    private final ObservableList<Auction> auctionList = FXCollections.observableArrayList();
    private final ObservableList<Auction> myWinsList = FXCollections.observableArrayList();
    private final ServerConnection conn = ServerConnection.getInstance();
    private Consumer<Response> realtimeListener;
    // Map itemId → name dùng để render cột "Sản phẩm" — server trả kèm response
    private final Map<Integer, String> itemNames = new HashMap<>();
    private final Map<Integer, String> myWinsItemNames = new HashMap<>();

    @FXML
    public void initialize() {
        // Setup header
        lblWelcome.setText("Xin chào, " + conn.getCurrentUsername());
        updateBalance();

        // Setup filter
        cboStatusFilter.getItems().addAll("Tất cả", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED");
        cboStatusFilter.setValue("Tất cả");
        cboStatusFilter.setOnAction(e -> loadAuctions());

        // Setup table columns
        colItem.setCellValueFactory(data -> {
            int itemId = data.getValue().getItemId();
            String name = itemNames.get(itemId);
            return new SimpleStringProperty(name != null ? name : "Item #" + itemId);
        });
        colPrice.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyFormatter.format(data.getValue().getCurrentPrice())));
        colBids.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getBidCount())));
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStatus().name()));
        if (colStartTime != null) {
            colStartTime.setCellValueFactory(data -> new SimpleStringProperty(
                    DateTimeUtil.formatDisplay(data.getValue().getStartTime())));
        }
        colEndTime.setCellValueFactory(data -> new SimpleStringProperty(
                DateTimeUtil.formatDisplay(data.getValue().getEndTime())));

        tableAuctions.setItems(auctionList);

        // Double-click vào row → mở chi tiết
        tableAuctions.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openAuctionDetail(row.getItem());
                }
            });
            return row;
        });

        // Setup tab "Phiên đã thắng"
        colWinItem.setCellValueFactory(data -> {
            int itemId = data.getValue().getItemId();
            String name = myWinsItemNames.get(itemId);
            return new SimpleStringProperty(name != null ? name : "Item #" + itemId);
        });
        colWinPrice.setCellValueFactory(data -> new SimpleStringProperty(
                MoneyFormatter.format(data.getValue().getCurrentPrice())));
        colWinStatus.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStatus().name().equals("FINISHED")
                        ? "Chờ thanh toán" : "Đã thanh toán"));
        colWinEndTime.setCellValueFactory(data -> new SimpleStringProperty(
                DateTimeUtil.formatDisplay(data.getValue().getEndTime())));
        tableMyWins.setItems(myWinsList);
        tableMyWins.setRowFactory(tv -> {
            TableRow<Auction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openAuctionDetail(row.getItem());
                }
            });
            return row;
        });

        // Ẩn nút seller/admin nếu không phải role đó
        String role = conn.getCurrentRole();
        if (btnSellerPanel != null) {
            btnSellerPanel.setVisible("SELLER".equals(role) || "ADMIN".equals(role));
        }
        if (btnAdminPanel != null) {
            btnAdminPanel.setVisible("ADMIN".equals(role));
        }

        // Đăng ký listener realtime — giữ reference để remove đúng về sau
        // (method reference mỗi lần tạo 1 Consumer mới nên không remove được nếu không lưu).
        // Guard: nếu user navigate qua lại (Dashboard → SellerPanel → Dashboard) mà
        // controller cũ chưa kịp gọi removeEventListener (ví dụ chỉ navigate, không logout),
        // thì initialize() lần 2 sẽ remove listener cũ trước khi add mới — tránh duplicate.
        if (realtimeListener != null) conn.removeEventListener(realtimeListener);
        realtimeListener = this::handleRealtimeEvent;
        conn.addEventListener(realtimeListener);

        // Load dữ liệu
        loadAuctions();
        loadMyWins();
    }

    /**
     * Load danh sách phiên đấu giá từ server.
     */
    @FXML
    private void loadAuctions() {
        new Thread(() -> {
            try {
                String filter = cboStatusFilter.getValue();
                String keyword = txtSearch != null ? txtSearch.getText().trim() : "";

                Response response;
                if (!"Tất cả".equals(filter)) {
                    Request req = new Request(CommandType.GET_AUCTIONS_BY_STATUS);
                    req.put("status", filter);
                    response = conn.sendRequest(req);
                } else {
                    response = conn.send(CommandType.GET_ALL_AUCTIONS);
                }

                if (response.isSuccess()) {
                    String json = response.getString("auctions");
                    List<Auction> auctions = JsonUtil.getGson().fromJson(json,
                            new TypeToken<List<Auction>>() {}.getType());

                    // Parse map itemId → name
                    Map<Integer, String> names = parseItemNames(response.getString("itemNames"));

                    // Lọc theo keyword nếu có (tìm cả theo tên item)
                    if (!keyword.isEmpty()) {
                        String kw = keyword.toLowerCase();
                        auctions.removeIf(a -> {
                            String n = names.get(a.getItemId());
                            return !String.valueOf(a.getItemId()).contains(kw)
                                    && !String.valueOf(a.getId()).contains(kw)
                                    && (n == null || !n.toLowerCase().contains(kw));
                        });
                    }

                    Platform.runLater(() -> {
                        itemNames.clear();
                        itemNames.putAll(names);
                        auctionList.clear();
                        auctionList.addAll(auctions);
                        tableAuctions.refresh();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Lỗi", "Không thể tải danh sách: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        loadAuctions();
    }

    /**
     * Load danh sách phiên user đã thắng.
     */
    @FXML
    private void loadMyWins() {
        new Thread(() -> {
            try {
                Response response = conn.send(CommandType.GET_MY_WINS);
                if (response.isSuccess()) {
                    String json = response.getString("auctions");
                    List<Auction> auctions = JsonUtil.getGson().fromJson(json,
                            new TypeToken<List<Auction>>() {}.getType());
                    Map<Integer, String> names = parseItemNames(response.getString("itemNames"));
                    Platform.runLater(() -> {
                        myWinsItemNames.clear();
                        myWinsItemNames.putAll(names);
                        myWinsList.clear();
                        myWinsList.addAll(auctions);
                        tableMyWins.refresh();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() ->
                        AlertUtil.showError("Lỗi", "Không thể tải danh sách phiên đã thắng: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Parse JSON string `{"1":"name1","2":"name2"}` → Map<Integer,String>.
     * Trả về map rỗng nếu json null/empty/lỗi.
     */
    private Map<Integer, String> parseItemNames(String json) {
        Map<Integer, String> result = new HashMap<>();
        if (json == null || json.isEmpty()) return result;
        try {
            Map<String, String> raw = JsonUtil.getGson().fromJson(json,
                    new TypeToken<Map<String, String>>() {}.getType());
            if (raw != null) {
                for (Map.Entry<String, String> e : raw.entrySet()) {
                    try { result.put(Integer.parseInt(e.getKey()), e.getValue()); }
                    catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    /**
     * Mở màn hình chi tiết phiên đấu giá.
     */
    private void openAuctionDetail(Auction auction) {
        AuctionDetailController.setSelectedAuction(auction);
        SceneManager.getInstance().switchScene("auction_detail.fxml", "Phiên đấu giá #" + auction.getId(), 1000, 700);
    }

    /**
     * Cleanup listener trước khi navigate sang scene khác — tránh leak ở
     * ServerConnection.eventListeners khi user quay lại Dashboard và initialize() tạo
     * controller mới (controller cũ bị GC nhưng listener cũ vẫn nằm trong CopyOnWriteArrayList).
     */
    private void cleanupBeforeNavigate() {
        if (realtimeListener != null) {
            conn.removeEventListener(realtimeListener);
            realtimeListener = null;
        }
    }

    @FXML
    private void handleSellerPanel() {
        cleanupBeforeNavigate();
        SceneManager.getInstance().switchScene("seller_panel.fxml", "Quản lý sản phẩm", 1100, 750);
    }

    @FXML
    private void handleAdminPanel() {
        cleanupBeforeNavigate();
        SceneManager.getInstance().switchScene("admin_panel.fxml", "Quản trị hệ thống", 1100, 750);
    }

    @FXML
    private void handleProfile() {
        cleanupBeforeNavigate();
        SceneManager.getInstance().switchScene("profile.fxml", "Tài khoản", 600, 700);
    }

    @FXML
    private void handleTopUp() {
        AlertUtil.showInput("Nạp tiền", "Nhập số tiền muốn nạp (VD: 100,000):", "100,000")
                .ifPresent(amountStr -> {
                    double amount;
                    try {
                        amount = MoneyFormatter.parse(amountStr);
                    } catch (NumberFormatException e) {
                        AlertUtil.showError("Lỗi", "Số tiền không hợp lệ");
                        return;
                    }
                    if (amount <= 0) {
                        AlertUtil.showError("Lỗi", "Số tiền phải lớn hơn 0");
                        return;
                    }
                    new Thread(() -> {
                        try {
                            Request req = new Request(CommandType.TOP_UP);
                            req.put("amount", amount);
                            Response resp = conn.sendRequest(req);
                            Platform.runLater(() -> {
                                if (resp.isSuccess()) {
                                    conn.setCurrentBalance(resp.getDouble("balance"));
                                    updateBalance();
                                    AlertUtil.showInfo("Thành công", resp.getMessage());
                                } else {
                                    AlertUtil.showError("Lỗi", resp.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
                        }
                    }).start();
                });
    }

    @FXML
    private void handleLogout() {
        if (realtimeListener != null) conn.removeEventListener(realtimeListener);
        new Thread(() -> {
            conn.disconnect();
            Platform.runLater(() ->
                    SceneManager.getInstance().switchScene("login.fxml", "Đăng nhập", 500, 500));
        }).start();
    }

    private void updateBalance() {
        lblBalance.setText("Số dư: " + MoneyFormatter.format(conn.getCurrentBalance()));
    }

    /**
     * Xử lý thông báo realtime từ server.
     */
    private void handleRealtimeEvent(Response response) {
        if (response.getCommand() == CommandType.AUCTION_ENDED) {
            // Phiên vừa kết thúc → refresh cả 2 tab (biết đâu user vừa thắng)
            Platform.runLater(() -> {
                loadAuctions();
                loadMyWins();
            });
        } else if (response.getCommand() == CommandType.BID_UPDATE) {
            Platform.runLater(this::loadAuctions);
        } else if (response.getCommand() == CommandType.BALANCE_UPDATE) {
            // Balance vừa thay đổi (refund cọc / payment / nhận tiền seller) → refresh label
            double newBalance = response.getDouble("balance");
            conn.setCurrentBalance(newBalance);
            Platform.runLater(this::updateBalance);
        }
    }
}
