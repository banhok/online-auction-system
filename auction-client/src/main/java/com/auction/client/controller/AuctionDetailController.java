package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.util.DateTimeUtil;
import com.auction.common.util.JsonUtil;
import com.auction.common.model.AuctionStatus;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.CountdownTimer;
import com.auction.client.util.MoneyFormatter;
import com.auction.client.util.SceneManager;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * AuctionDetailController — màn hình chi tiết phiên đấu giá.
 * Hiển thị: thông tin, đếm ngược, đặt giá, lịch sử bid, biểu đồ giá realtime.
 */
public class AuctionDetailController {

    @FXML private Label lblAuctionId;
    @FXML private Label lblItemName;
    @FXML private Label lblItemDetails;
    @FXML private Label lblExtendedBadge;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBidCount;
    @FXML private Label lblParticipantCount;
    @FXML private Label lblStatus;
    @FXML private Label lblCountdown;
    @FXML private Label lblStartEndTime;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnJoinAuction;
    @FXML private Button btnPay;
    @FXML private Button btnBack;
    @FXML private TableView<BidTransaction> tableBidHistory;
    @FXML private TableColumn<BidTransaction, String> colBidder;
    @FXML private TableColumn<BidTransaction, String> colAmount;
    @FXML private TableColumn<BidTransaction, String> colTime;
    @FXML private LineChart<Number, Number> chartPrice;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private static Auction selectedAuction;
    private final ServerConnection conn = ServerConnection.getInstance();
    private final ObservableList<BidTransaction> bidList = FXCollections.observableArrayList();
    private CountdownTimer countdownTimer;
    private XYChart.Series<Number, Number> priceSeries;
    private Consumer<Response> realtimeListener;
    // Map bidderId → tên hiển thị (server gửi qua bidderNames trong GET_BID_HISTORY + bidderName ở BID_UPDATE)
    private final Map<Integer, String> bidderNames = new HashMap<>();

    public static void setSelectedAuction(Auction auction) {
        selectedAuction = auction;
    }

    @FXML
    public void initialize() {
        if (selectedAuction == null) {
            AlertUtil.showError("Lỗi", "Không tìm thấy phiên đấu giá");
            handleBack();
            return;
        }

        // Hiển thị thông tin
        updateAuctionDisplay();

        // Setup table — hiển thị tên người đặt thay vì "User #X"
        colBidder.setCellValueFactory(data -> {
            int bidderId = data.getValue().getBidderId();
            String name = bidderNames.get(bidderId);
            return new javafx.beans.property.SimpleStringProperty(
                    name != null ? name : "User #" + bidderId);
        });
        colAmount.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(MoneyFormatter.format(data.getValue().getBidAmount())));
        colTime.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(DateTimeUtil.formatDisplay(data.getValue().getBidTime())));
        tableBidHistory.setItems(bidList);

        // Format input tiền (phân cách hàng nghìn bằng dấu phẩy)
        if (txtBidAmount != null) MoneyFormatter.attachTo(txtBidAmount);

        // Load chi tiết sản phẩm (polymorphism: server gọi getCategoryDetails() runtime)
        loadItemDetails();

        // Load số người tham gia ban đầu (sau đó tự refresh qua BID_UPDATE)
        loadParticipantCount();

        // Hiển thị badge "đã gia hạn" nếu endTime > originalEndTime
        updateExtendedBadge();

        // Cập nhật hiển thị nút pay theo trạng thái phiên
        updatePaymentButton();

        // Setup biểu đồ giá realtime — trục X = giây trôi qua kể từ startTime
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá đấu");
        chartPrice.getData().add(priceSeries);
        chartPrice.setCreateSymbols(true);
        chartPrice.setAnimated(false);
        configureChartAxes();

        // Đồng hồ đếm ngược
        countdownTimer = new CountdownTimer(lblCountdown);
        if (selectedAuction.getEndTime() != null) {
            countdownTimer.start(selectedAuction.getEndTime(), () -> {
                Platform.runLater(() -> lblStatus.setText("Đã kết thúc"));
            });
        }

        // Subscribe realtime updates
        subscribeAuction();

        // Load bid history
        loadBidHistory();

        // Đăng ký listener realtime
        realtimeListener = this::handleRealtimeEvent;
        conn.addEventListener(realtimeListener);
    }

    private void updateAuctionDisplay() {
        lblAuctionId.setText("Phiên #" + selectedAuction.getId());
        lblItemName.setText("Item #" + selectedAuction.getItemId());
        lblStartingPrice.setText("Giá khởi điểm: " + MoneyFormatter.format(selectedAuction.getStartingPrice()));
        lblCurrentPrice.setText(MoneyFormatter.format(selectedAuction.getCurrentPrice()));
        lblBidCount.setText(selectedAuction.getBidCount() + " lượt đấu giá");
        lblStatus.setText(selectedAuction.getStatus().name());
        if (lblStartEndTime != null) {
            lblStartEndTime.setText(
                    "🟢 " + DateTimeUtil.formatDisplay(selectedAuction.getStartTime())
                    + "  →  🔴 " + DateTimeUtil.formatDisplay(selectedAuction.getEndTime()));
        }
    }

    /**
     * Load lịch sử bid từ server.
     */
    private void loadBidHistory() {
        new Thread(() -> {
            try {
                Request req = new Request(CommandType.GET_BID_HISTORY);
                req.put("auctionId", selectedAuction.getId());
                Response resp = conn.sendRequest(req);

                if (resp.isSuccess()) {
                    List<BidTransaction> bids = JsonUtil.getGson().fromJson(
                            resp.getString("bids"),
                            new TypeToken<List<BidTransaction>>() {}.getType());

                    Map<Integer, String> names = parseIdNameMap(resp.getString("bidderNames"));

                    Platform.runLater(() -> {
                        bidderNames.clear();
                        bidderNames.putAll(names);
                        bidList.clear();
                        bidList.addAll(bids);
                        tableBidHistory.refresh();

                        // Cập nhật biểu đồ — X = giây trôi qua kể từ startTime
                        priceSeries.getData().clear();
                        for (BidTransaction bid : bids) {
                            priceSeries.getData().add(new XYChart.Data<>(
                                    elapsedSeconds(bid.getBidTime()), bid.getBidAmount()));
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    /**
     * Đặt cọc tham gia phiên đấu giá.
     */
    @FXML
    private void handleJoinAuction() {
        if (!AlertUtil.showConfirm("Xác nhận",
                "Đặt cọc " + MoneyFormatter.format(selectedAuction.getDepositAmount())
                        + " để tham gia phiên đấu giá?")) {
            return;
        }

        new Thread(() -> {
            try {
                Request req = new Request(CommandType.JOIN_AUCTION);
                req.put("auctionId", selectedAuction.getId());
                Response resp = conn.sendRequest(req);

                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        AlertUtil.showInfo("Thành công", "Đã tham gia đấu giá!");
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    /**
     * Đặt giá.
     */
    @FXML
    private void handlePlaceBid() {
        String amountStr = txtBidAmount.getText().trim();
        if (amountStr.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập số tiền");
            return;
        }

        double amount;
        try {
            amount = MoneyFormatter.parse(amountStr);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Số tiền không hợp lệ");
            return;
        }

        if (!AlertUtil.showConfirm("Xác nhận đặt giá",
                "Bạn chắc chắn muốn đặt giá " + MoneyFormatter.format(amount) + "?")) {
            return;
        }

        btnPlaceBid.setDisable(true);

        new Thread(() -> {
            try {
                Request req = new Request(CommandType.PLACE_BID);
                req.put("auctionId", selectedAuction.getId());
                req.put("bidAmount", amount);
                Response resp = conn.sendRequest(req);

                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    if (resp.isSuccess()) {
                        txtBidAmount.clear();
                        AlertUtil.showInfo("Thành công", "Đặt giá thành công!");
                    } else {
                        AlertUtil.showError("Đặt giá thất bại", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnPlaceBid.setDisable(false);
                    AlertUtil.showError("Lỗi", e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Subscribe nhận update realtime cho phiên này.
     */
    private void subscribeAuction() {
        new Thread(() -> {
            try {
                Request req = new Request(CommandType.SUBSCRIBE_AUCTION);
                req.put("auctionId", selectedAuction.getId());
                conn.sendRequest(req);
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }

    /**
     * Xử lý event realtime từ server.
     */
    private void handleRealtimeEvent(Response response) {
        if (response.get("auctionId") == null) return;

        int eventAuctionId;
        Object aid = response.get("auctionId");
        if (aid instanceof Number) {
            eventAuctionId = ((Number) aid).intValue();
        } else {
            return;
        }

        if (eventAuctionId != selectedAuction.getId()) return;

        Platform.runLater(() -> {
            switch (response.getCommand()) {
                case BID_UPDATE:
                    // Cập nhật giá hiện tại
                    double newPrice = response.getDouble("currentPrice");
                    int newBidCount = response.getInt("bidCount");
                    selectedAuction.setCurrentPrice(newPrice);
                    selectedAuction.setBidCount(newBidCount);
                    lblCurrentPrice.setText(MoneyFormatter.format(newPrice));
                    lblBidCount.setText(newBidCount + " lượt đấu giá");
                    updateParticipantLabel(response.get("participantCount"));

                    // Cache tên bidder cho table hiển thị + chart label
                    Object bidderIdObj = response.get("bidderId");
                    String bidderName = response.getString("bidderName");
                    if (bidderIdObj instanceof Number && bidderName != null) {
                        bidderNames.put(((Number) bidderIdObj).intValue(), bidderName);
                    }

                    // Thêm điểm vào biểu đồ (realtime) — X = giây trôi qua kể từ startTime.
                    // Ưu tiên dùng bidTimeDb từ response; fallback now nếu thiếu.
                    String bidTimeDbStr = response.getString("bidTimeDb");
                    LocalDateTime bidTime = bidTimeDbStr != null
                            ? DateTimeUtil.parseDb(bidTimeDbStr) : LocalDateTime.now();
                    priceSeries.getData().add(new XYChart.Data<>(
                            elapsedSeconds(bidTime), newPrice));

                    // Reload bid history
                    loadBidHistory();

                    // Hiệu ứng nhấp nháy (dùng PauseTransition thay Thread.sleep)
                    lblCurrentPrice.getStyleClass().add("price-flash");
                    javafx.animation.PauseTransition flashPause =
                            new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                    flashPause.setOnFinished(e -> lblCurrentPrice.getStyleClass().remove("price-flash"));
                    flashPause.play();
                    break;

                case AUCTION_EXTENDED:
                    // Cập nhật thời gian kết thúc mới
                    String newEndTimeStr = response.getString("endTime");
                    if (newEndTimeStr != null) {
                        selectedAuction.setEndTime(DateTimeUtil.parseDb(newEndTimeStr));
                        countdownTimer.updateEndTime(selectedAuction.getEndTime());
                        updateExtendedBadge();
                        // Mở rộng upper bound trục X cho phù hợp với endTime mới
                        configureChartAxes();
                        AlertUtil.showInfo("Gia hạn", "Phiên đấu giá được gia hạn thêm!");
                    }
                    break;

                case AUCTION_ENDED:
                    selectedAuction.setStatus(AuctionStatus.FINISHED);
                    lblStatus.setText("FINISHED");
                    countdownTimer.stop();
                    btnPlaceBid.setDisable(true);
                    btnJoinAuction.setDisable(true);
                    updateParticipantLabel(response.get("participantCount"));

                    Integer winnerId = null;
                    Object wid = response.get("winnerId");
                    if (wid instanceof Number) {
                        winnerId = ((Number) wid).intValue();
                    }
                    double finalPrice = response.getDouble("finalPrice");
                    if (finalPrice > 0) {
                        selectedAuction.setCurrentPrice(finalPrice);
                        lblCurrentPrice.setText(MoneyFormatter.format(finalPrice));
                    }

                    // Winner được thiết lập → cập nhật nút Thanh toán
                    if (winnerId != null && winnerId > 0) {
                        selectedAuction.setWinnerId(winnerId);
                        updatePaymentButton();
                    }

                    String winnerName = response.getString("winnerName");
                    String winnerLabel = winnerName != null ? winnerName : "User #" + winnerId;

                    if (winnerId == null || winnerId <= 0) {
                        AlertUtil.showInfo("Phiên kết thúc",
                                "Phiên đấu giá đã kết thúc — không có người thắng.");
                    } else if (winnerId == conn.getCurrentUserId()) {
                        AlertUtil.showInfo("🎉 Chúc mừng!",
                                "Bạn đã thắng phiên đấu giá với giá "
                                        + MoneyFormatter.format(finalPrice) + "!");
                    } else {
                        AlertUtil.showInfo("Phiên kết thúc",
                                "🏆 Người thắng: " + winnerLabel
                                        + "\nGiá chốt: " + MoneyFormatter.format(finalPrice));
                    }
                    break;

                default:
                    break;
            }
        });
    }

    /**
     * Cấu hình trục X = giây trôi qua kể từ startTime, label dạng "mm:ss".
     * Gọi lại khi anti-snipe gia hạn endTime để mở rộng upper bound.
     */
    private void configureChartAxes() {
        if (xAxis == null) return;
        xAxis.setLabel("Thời gian (mm:ss)");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);

        long totalSec = 0;
        if (selectedAuction.getStartTime() != null && selectedAuction.getEndTime() != null) {
            totalSec = Math.max(0,
                    Duration.between(selectedAuction.getStartTime(),
                            selectedAuction.getEndTime()).getSeconds());
        }
        // Tránh upperBound = 0 (gây render lỗi axis) — fallback 60s nếu phiên chưa hợp lệ
        if (totalSec <= 0) totalSec = 60;

        xAxis.setUpperBound(totalSec);
        xAxis.setTickUnit(Math.max(10, totalSec / 10.0));
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override public String toString(Number n) {
                long s = n.longValue();
                return String.format("%d:%02d", s / 60, s % 60);
            }
            @Override public Number fromString(String s) { return 0; }
        });

        if (yAxis != null) yAxis.setLabel("Giá (VNĐ)");
    }

    /**
     * Parse JSON map {"id": "name"} → Map<Integer,String> cho bidderNames/itemNames.
     */
    private Map<Integer, String> parseIdNameMap(String json) {
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
     * Số giây trôi qua từ startTime tới bidTime, clamp tối thiểu 0.
     */
    private long elapsedSeconds(LocalDateTime bidTime) {
        if (bidTime == null || selectedAuction.getStartTime() == null) return 0;
        return Math.max(0,
                Duration.between(selectedAuction.getStartTime(), bidTime).getSeconds());
    }

    /**
     * Cập nhật label "X người tham gia" từ giá trị Number/Integer trong response.
     * No-op nếu data thiếu — giữ giá trị cũ.
     */
    private void updateParticipantLabel(Object countObj) {
        if (lblParticipantCount == null || !(countObj instanceof Number)) return;
        int count = ((Number) countObj).intValue();
        lblParticipantCount.setText("👥 " + count + " người tham gia");
    }

    /**
     * Load số người tham gia ban đầu + tên item từ GET_AUCTION khi mới mở màn hình.
     */
    private void loadParticipantCount() {
        new Thread(() -> {
            try {
                Request req = new Request(CommandType.GET_AUCTION);
                req.put("auctionId", selectedAuction.getId());
                Response resp = conn.sendRequest(req);
                if (resp.isSuccess()) {
                    Object count = resp.get("participantCount");
                    String itemName = resp.getString("itemName");
                    Platform.runLater(() -> {
                        updateParticipantLabel(count);
                        if (itemName != null && lblItemName != null) {
                            lblItemName.setText(itemName);
                        }
                    });
                }
            } catch (Exception ignored) {
                // Label fallback giữ "Item #X"
            }
        }).start();
    }

    /**
     * Load chi tiết sản phẩm (server đã gọi Item.getCategoryDetails() polymorphism).
     */
    private void loadItemDetails() {
        new Thread(() -> {
            try {
                Request req = new Request(CommandType.GET_ITEM);
                req.put("itemId", selectedAuction.getItemId());
                Response resp = conn.sendRequest(req);
                if (resp.isSuccess()) {
                    String details = resp.getString("categoryDetails");
                    Platform.runLater(() -> {
                        if (lblItemDetails != null && details != null) {
                            lblItemDetails.setText(details);
                        }
                    });
                }
            } catch (Exception ignored) {
                // Chi tiết chỉ hiển thị thêm — không crash UI
            }
        }).start();
    }

    /**
     * Hiển thị badge "đã gia hạn" nếu endTime > originalEndTime.
     */
    private void updateExtendedBadge() {
        if (lblExtendedBadge == null) return;
        boolean extended = selectedAuction.getOriginalEndTime() != null
                && selectedAuction.getEndTime() != null
                && selectedAuction.getEndTime().isAfter(selectedAuction.getOriginalEndTime());
        lblExtendedBadge.setVisible(extended);
        lblExtendedBadge.setManaged(extended);
    }

    /**
     * Chỉ winner mới thấy nút pay; ẩn khi phiên đã PAID hoặc chưa kết thúc.
     */
    private void updatePaymentButton() {
        if (btnPay == null) return;
        boolean canPay = selectedAuction.getStatus() == AuctionStatus.FINISHED
                && selectedAuction.getWinnerId() != null
                && selectedAuction.getWinnerId() == conn.getCurrentUserId();
        btnPay.setVisible(canPay);
        btnPay.setManaged(canPay);
    }

    /**
     * Winner thanh toán sau khi thắng phiên.
     */
    @FXML
    private void handlePay() {
        if (!AlertUtil.showConfirm("Xác nhận thanh toán",
                "Thanh toán " + MoneyFormatter.format(selectedAuction.getCurrentPrice())
                        + " cho phiên này? (cọc đã đặt sẽ được trừ vào tổng)")) {
            return;
        }

        btnPay.setDisable(true);
        new Thread(() -> {
            try {
                Request req = new Request(CommandType.PAY_WINNER);
                req.put("auctionId", selectedAuction.getId());
                Response resp = conn.sendRequest(req);

                Platform.runLater(() -> {
                    btnPay.setDisable(false);
                    if (resp.isSuccess()) {
                        selectedAuction.setStatus(AuctionStatus.PAID);
                        lblStatus.setText("PAID");
                        btnPay.setVisible(false);
                        btnPay.setManaged(false);
                        AlertUtil.showInfo("Thành công", "Thanh toán thành công!");
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnPay.setDisable(false);
                    AlertUtil.showError("Lỗi", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleBack() {
        // Cleanup
        if (countdownTimer != null) countdownTimer.stop();
        if (realtimeListener != null) conn.removeEventListener(realtimeListener);
        SceneManager.getInstance().switchScene("dashboard.fxml", "Dashboard", 1200, 800);
    }
}
