package com.auction.client.controller;

import com.auction.common.command.CommandType;
import com.auction.common.dto.Request;
import com.auction.common.dto.Response;
import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import com.auction.common.util.JsonUtil;
import com.auction.client.network.ServerConnection;
import com.auction.client.util.AlertUtil;
import com.auction.client.util.AvatarInitials;
import com.auction.client.util.ImageLoader;
import com.auction.client.util.LoadingOverlay;
import com.auction.client.util.ModalDialog;
import com.auction.client.util.MoneyFormatter;
import com.auction.client.util.SceneManager;
import com.auction.client.util.StatusBadge;
import com.auction.client.util.ToastUtil;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SellerPanelController — quản lý sản phẩm và phiên đấu giá cho Seller.
 */
public class SellerPanelController {

    // --- Tab Tạo sản phẩm ---
    @FXML private TextField txtItemName;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cboCategory;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtImageUrl;
    // Extra fields
    @FXML private TextField txtExtraField1;
    @FXML private TextField txtExtraField2;
    @FXML private TextField txtExtraField3;
    @FXML private TextField txtExtraField4;
    @FXML private Label lblExtra1;
    @FXML private Label lblExtra2;
    @FXML private Label lblExtra3;
    @FXML private Label lblExtra4;

    // --- Tab Tạo phiên đấu giá ---
    @FXML private ComboBox<Item> cboAuctionItem;
    @FXML private TextField txtAuctionStartPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private Spinner<Integer> spStartHour;
    @FXML private Spinner<Integer> spStartMinute;
    @FXML private DatePicker dpEndDate;
    @FXML private Spinner<Integer> spEndHour;
    @FXML private Spinner<Integer> spEndMinute;

    // --- Tab Phiên của tôi ---
    @FXML private TableView<Auction> tableMyAuctions;
    @FXML private TableColumn<Auction, String> colAucItem;
    @FXML private TableColumn<Auction, String> colAucPrice;
    @FXML private TableColumn<Auction, String> colAucBids;
    @FXML private TableColumn<Auction, String> colAucStatus;
    @FXML private TableColumn<Auction, String> colAucStart;
    @FXML private TableColumn<Auction, String> colAucEnd;
    @FXML private TableColumn<Auction, Void> colAucCancel;

    // --- Tab Sản phẩm của tôi ---
    @FXML private TableView<Item> tableMyItems;
    @FXML private TableColumn<Item, String> colItemName;
    @FXML private TableColumn<Item, String> colItemCategory;
    @FXML private TableColumn<Item, String> colItemPrice;
    @FXML private TableColumn<Item, String> colItemStatus;
    @FXML private TableColumn<Item, Void> colItemAction;

    @FXML private Button btnBack;
    @FXML private StackPane avatarSlot;
    @FXML private ImageView imgItemPreview;

    private final ServerConnection conn = ServerConnection.getInstance();
    private final ObservableList<Auction> myAuctions = FXCollections.observableArrayList();
    private final ObservableList<Item> myItems = FXCollections.observableArrayList();
    // Map itemId → trạng thái auction (để render cột "Trạng thái" của tab Sản phẩm)
    private final Map<Integer, com.auction.common.model.AuctionStatus> itemAuctionStatus = new HashMap<>();

    @FXML
    public void initialize() {
        // Avatar top bar
        if (avatarSlot != null) {
            avatarSlot.getChildren().setAll(AvatarInitials.create(conn.getCurrentUsername(), 36));
        }

        // Item preview image — initial placeholder + listener khi paste URL
        if (imgItemPreview != null) {
            imgItemPreview.setImage(ImageLoader.placeholder());
            if (txtImageUrl != null) {
                txtImageUrl.textProperty().addListener((obs, oldVal, newVal) -> {
                    String url = newVal == null ? "" : newVal.trim();
                    imgItemPreview.setImage(ImageLoader.load(url, 200, 200));
                });
            }
        }

        // Setup category dropdown
        cboCategory.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        cboCategory.setValue("ELECTRONICS");
        cboCategory.setOnAction(e -> updateExtraFields());
        updateExtraFields();

        // Setup table phiên
        if (tableMyAuctions != null) {
            colAucItem.setCellValueFactory(d -> {
                int itemId = d.getValue().getItemId();
                Item item = findItemById(itemId);
                return new SimpleStringProperty(item != null ? item.getName() : "Item #" + itemId);
            });
            colAucPrice.setCellValueFactory(d -> new SimpleStringProperty(
                    MoneyFormatter.format(d.getValue().getCurrentPrice())));
            colAucBids.setCellValueFactory(d -> new SimpleStringProperty(
                    String.valueOf(d.getValue().getBidCount())));
            colAucStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
            colAucStatus.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Auction a = (Auction) getTableRow().getItem();
                    setGraphic(StatusBadge.create(a.getStatus()));
                    setText(null);
                }
            });
            if (colAucStart != null) {
                colAucStart.setCellValueFactory(d -> new SimpleStringProperty(
                        com.auction.common.util.DateTimeUtil.formatDisplay(d.getValue().getStartTime())));
            }
            if (colAucEnd != null) {
                colAucEnd.setCellValueFactory(d -> new SimpleStringProperty(
                        com.auction.common.util.DateTimeUtil.formatDisplay(d.getValue().getEndTime())));
            }
            addCancelButtonToAuctionsTable();
            tableMyAuctions.setItems(myAuctions);
        }

        // Setup table sản phẩm của tôi
        if (tableMyItems != null) {
            colItemName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
            colItemCategory.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getCategory() != null ? d.getValue().getCategory().name() : ""));
            colItemPrice.setCellValueFactory(d -> new SimpleStringProperty(
                    MoneyFormatter.format(d.getValue().getStartingPrice())));
            if (colItemStatus != null) {
                colItemStatus.setCellValueFactory(d -> new SimpleStringProperty(
                        formatItemStatus(itemAuctionStatus.get(d.getValue().getId()))));
                colItemStatus.setCellFactory(col -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                            setGraphic(null);
                            setText(null);
                            return;
                        }
                        Item it = (Item) getTableRow().getItem();
                        com.auction.common.model.AuctionStatus s = itemAuctionStatus.get(it.getId());
                        setGraphic(StatusBadge.create(s, item));
                        setText(null);
                    }
                });
            }
            addDeleteButtonToItemsTable();
            tableMyItems.setItems(myItems);
        }

        // Format input tiền ở tab tạo sản phẩm + tạo phiên
        if (txtStartingPrice != null) MoneyFormatter.attachTo(txtStartingPrice);
        if (txtAuctionStartPrice != null) MoneyFormatter.attachTo(txtAuctionStartPrice);

        // Setup ComboBox chọn item
        if (cboAuctionItem != null) {
            Callback<ListView<Item>, ListCell<Item>> cellFactory = lv -> new ListCell<>() {
                @Override protected void updateItem(Item it, boolean empty) {
                    super.updateItem(it, empty);
                    setText(empty || it == null ? null
                            : "#" + it.getId() + " · " + it.getName()
                              + (it.getCategory() != null ? " (" + it.getCategory().name() + ")" : ""));
                }
            };
            cboAuctionItem.setCellFactory(cellFactory);
            cboAuctionItem.setButtonCell(cellFactory.call(null));

            // Auto-fill startingPrice khi user chọn item (vẫn cho phép override sau đó).
            // Dùng formatPlain (không hậu tố " VNĐ") vì txtAuctionStartPrice gắn TextFormatter
            // chỉ accept chữ số + dấu phẩy — format() có "VNĐ" sẽ bị filter reject → field rỗng.
            cboAuctionItem.valueProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && txtAuctionStartPrice != null) {
                    txtAuctionStartPrice.setText(MoneyFormatter.formatPlain(newItem.getStartingPrice()));
                }
            });
        }

        // Default time: start = now + 5 phút, end = now + 1 giờ
        LocalDateTime defaultStart = LocalDateTime.now().plusMinutes(5).withSecond(0).withNano(0);
        LocalDateTime defaultEnd = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0);
        if (dpStartDate != null) dpStartDate.setValue(defaultStart.toLocalDate());
        if (dpEndDate != null) dpEndDate.setValue(defaultEnd.toLocalDate());
        if (spStartHour != null) spStartHour.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, defaultStart.getHour()));
        if (spStartMinute != null) spStartMinute.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, defaultStart.getMinute()));
        if (spEndHour != null) spEndHour.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, defaultEnd.getHour()));
        if (spEndMinute != null) spEndMinute.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, defaultEnd.getMinute()));

        loadMyAuctions();
        loadMyItems();
    }

    /**
     * Cập nhật label cho extra fields dựa theo category.
     */
    private void updateExtraFields() {
        String cat = cboCategory.getValue();
        if (cat == null) return;

        switch (cat) {
            case "ELECTRONICS":
                setExtraLabels("Thương hiệu:", "Tình trạng (NEW/USED):", "Bảo hành (tháng):", "");
                break;
            case "ART":
                setExtraLabels("Tác giả:", "Năm sáng tác:", "Chất liệu:", "");
                break;
            case "VEHICLE":
                setExtraLabels("Hãng xe:", "Mẫu xe:", "Năm sản xuất:", "Số km:");
                break;
        }
    }

    private void setExtraLabels(String l1, String l2, String l3, String l4) {
        if (lblExtra1 != null) lblExtra1.setText(l1);
        if (lblExtra2 != null) lblExtra2.setText(l2);
        if (lblExtra3 != null) lblExtra3.setText(l3);
        if (lblExtra4 != null) {
            lblExtra4.setText(l4);
            lblExtra4.setVisible(!l4.isEmpty());
            if (txtExtraField4 != null) txtExtraField4.setVisible(!l4.isEmpty());
        }
    }

    /**
     * Tạo sản phẩm mới.
     */
    @FXML
    private void handleCreateItem() {
        String name = txtItemName.getText().trim();
        String desc = txtDescription.getText().trim();
        String category = cboCategory.getValue();
        String priceStr = txtStartingPrice.getText().trim();
        String imageUrl = txtImageUrl != null ? txtImageUrl.getText().trim() : "";

        if (name.isEmpty() || priceStr.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập tên và giá khởi điểm");
            return;
        }

        double price;
        try {
            price = MoneyFormatter.parse(priceStr);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Giá không hợp lệ");
            return;
        }

        // Build extra data
        Map<String, Object> extraData = buildExtraData(category);

        new Thread(() -> {
            try {
                Request req = new Request(CommandType.CREATE_ITEM);
                req.put("name", name);
                req.put("description", desc);
                req.put("category", category);
                req.put("startingPrice", price);
                req.put("imageUrl", imageUrl);
                req.put("extraData", extraData);
                Response resp = conn.sendRequest(req);

                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        ToastUtil.success(btnBack,
                                "✅ Đã tạo sản phẩm #" + resp.get("itemId"));
                        clearItemForm();
                        loadMyItems();
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    private Map<String, Object> buildExtraData(String category) {
        Map<String, Object> data = new HashMap<>();
        String f1 = txtExtraField1 != null ? txtExtraField1.getText().trim() : "";
        String f2 = txtExtraField2 != null ? txtExtraField2.getText().trim() : "";
        String f3 = txtExtraField3 != null ? txtExtraField3.getText().trim() : "";
        String f4 = txtExtraField4 != null ? txtExtraField4.getText().trim() : "";

        switch (category) {
            case "ELECTRONICS":
                data.put("brand", f1);
                data.put("condition", f2.isEmpty() ? "USED" : f2);
                data.put("warrantyMonths", f3.isEmpty() ? 0 : Integer.parseInt(f3));
                break;
            case "ART":
                data.put("artist", f1);
                data.put("year", f2.isEmpty() ? 2024 : Integer.parseInt(f2));
                data.put("medium", f3);
                break;
            case "VEHICLE":
                data.put("make", f1);
                data.put("modelName", f2);
                data.put("yearMade", f3.isEmpty() ? 2020 : Integer.parseInt(f3));
                data.put("mileage", f4.isEmpty() ? 0 : Integer.parseInt(f4));
                break;
        }
        return data;
    }

    /**
     * Tạo phiên đấu giá.
     */
    @FXML
    private void handleCreateAuction() {
        Item selected = cboAuctionItem.getValue();
        String priceStr = txtAuctionStartPrice.getText().trim();
        LocalDate startDate = dpStartDate.getValue();
        LocalDate endDate = dpEndDate.getValue();
        Integer startH = spStartHour.getValue();
        Integer startM = spStartMinute.getValue();
        Integer endH = spEndHour.getValue();
        Integer endM = spEndMinute.getValue();

        if (selected == null) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn sản phẩm");
            return;
        }
        if (priceStr.isEmpty()) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng nhập giá khởi điểm");
            return;
        }
        if (startDate == null || endDate == null
                || startH == null || startM == null || endH == null || endM == null) {
            AlertUtil.showWarning("Cảnh báo", "Vui lòng chọn đầy đủ ngày giờ bắt đầu và kết thúc");
            return;
        }

        double price;
        try {
            price = MoneyFormatter.parse(priceStr);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Lỗi", "Giá khởi điểm không hợp lệ");
            return;
        }

        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(startH, startM));
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.of(endH, endM));

        if (!endTime.isAfter(startTime)) {
            AlertUtil.showWarning("Cảnh báo", "Thời gian kết thúc phải sau thời gian bắt đầu");
            return;
        }
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
            AlertUtil.showWarning("Cảnh báo", "Thời gian bắt đầu không được ở quá khứ");
            return;
        }

        LoadingOverlay overlay = LoadingOverlay.show(txtAuctionStartPrice);
        new Thread(() -> {
            try {
                Request req = new Request(CommandType.CREATE_AUCTION);
                req.put("itemId", selected.getId());
                req.put("startingPrice", price);
                req.put("startTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                req.put("endTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                Response resp = conn.sendRequest(req);

                Platform.runLater(() -> {
                    overlay.hide();
                    if (resp.isSuccess()) {
                        ToastUtil.success(btnBack,
                                "🔨 Đã tạo phiên đấu giá #" + resp.get("auctionId"));
                        loadMyAuctions();
                        loadMyItems();
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    overlay.hide();
                    AlertUtil.showError("Lỗi", e.getMessage());
                });
            }
        }).start();
    }

    private void loadMyAuctions() {
        new Thread(() -> {
            try {
                Response resp = conn.send(CommandType.GET_AUCTIONS_BY_SELLER);
                if (resp.isSuccess()) {
                    List<Auction> auctions = JsonUtil.getGson().fromJson(
                            resp.getString("auctions"),
                            new TypeToken<List<Auction>>() {}.getType());
                    Platform.runLater(() -> {
                        myAuctions.clear();
                        myAuctions.addAll(auctions);
                    });
                }
            } catch (Exception e) {
                // Ignore
            }
        }).start();
    }

    /**
     * Load danh sách item của seller hiện tại — cập nhật cả table Tab 4 lẫn ComboBox Tab 2.
     * Đồng thời gọi GET_AUCTIONS_BY_SELLER để build map status cho cột "Trạng thái".
     */
    private void loadMyItems() {
        new Thread(() -> {
            try {
                Response itemsResp = conn.send(CommandType.GET_ITEMS_BY_SELLER);
                if (!itemsResp.isSuccess()) return;

                List<Item> items = JsonUtil.getGson().fromJson(
                        itemsResp.getString("items"),
                        new TypeToken<List<Item>>() {}.getType());

                // Lấy auction list để build map itemId → status (1 item ↔ 0/1 auction do unique)
                Map<Integer, com.auction.common.model.AuctionStatus> statusMap = new HashMap<>();
                try {
                    Response aucResp = conn.send(CommandType.GET_AUCTIONS_BY_SELLER);
                    if (aucResp.isSuccess()) {
                        List<Auction> aucs = JsonUtil.getGson().fromJson(
                                aucResp.getString("auctions"),
                                new TypeToken<List<Auction>>() {}.getType());
                        for (Auction a : aucs) {
                            statusMap.put(a.getItemId(), a.getStatus());
                        }
                    }
                } catch (Exception ignored) {
                    // Không có auction list cũng không sao — cột status fallback "Sẵn sàng"
                }

                Platform.runLater(() -> {
                    itemAuctionStatus.clear();
                    itemAuctionStatus.putAll(statusMap);
                    myItems.setAll(items);
                    if (tableMyItems != null) tableMyItems.refresh();
                    // Tab "Phiên của tôi" cũng cần refresh — cột "Sản phẩm" lookup theo myItems
                    if (tableMyAuctions != null) tableMyAuctions.refresh();

                    if (cboAuctionItem != null) {
                        Item prev = cboAuctionItem.getValue();
                        cboAuctionItem.getItems().setAll(items);
                        if (prev != null) {
                            cboAuctionItem.setValue(items.stream()
                                    .filter(i -> i.getId() == prev.getId())
                                    .findFirst().orElse(null));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi",
                        "Không tải được danh sách sản phẩm: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Tìm Item theo id trong list myItems hiện tại — dùng cho cột "Sản phẩm" tab Phiên của tôi.
     */
    private Item findItemById(int itemId) {
        for (Item it : myItems) {
            if (it.getId() == itemId) return it;
        }
        return null;
    }

    /**
     * Format AuctionStatus → text hiển thị tiếng Việt cho cột "Trạng thái" của item.
     * null = item chưa có auction.
     */
    private String formatItemStatus(com.auction.common.model.AuctionStatus status) {
        if (status == null) return "Sẵn sàng";
        switch (status) {
            case OPEN:     return "Sẵn sàng đấu giá";
            case RUNNING:  return "Đang đấu giá";
            case FINISHED: return "Chờ thanh toán";
            case PAID:     return "Đã bán";
            case CANCELED: return "Đã huỷ";
            default:       return status.name();
        }
    }

    @FXML
    private void handleRefreshItems() {
        loadMyItems();
    }

    /**
     * Cột "Thao tác" ở tab "Phiên của tôi" — nút Huỷ chỉ enable khi status OPEN/RUNNING.
     */
    private void addCancelButtonToAuctionsTable() {
        if (colAucCancel == null) return;
        colAucCancel.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("❌ Huỷ");
            {
                btn.getStyleClass().add("btn-danger");
                btn.setOnAction(e -> {
                    Auction a = getTableView().getItems().get(getIndex());
                    boolean ok = ModalDialog.confirm(
                            btn.getScene().getWindow(),
                            "Xác nhận huỷ phiên",
                            "Huỷ phiên #" + a.getId() + "?\n\n"
                                    + "⚠️ Toàn bộ tiền cọc của bidder sẽ được hoàn ngay lập tức.");
                    if (ok) cancelAuction(a.getId());
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Auction a = getTableView().getItems().get(getIndex());
                boolean canCancel = a != null
                        && (a.getStatus() == com.auction.common.model.AuctionStatus.OPEN
                            || a.getStatus() == com.auction.common.model.AuctionStatus.RUNNING);
                btn.setDisable(!canCancel);
                setGraphic(btn);
            }
        });
    }

    private void cancelAuction(int auctionId) {
        new Thread(() -> {
            try {
                Response resp = conn.send(CommandType.CANCEL_AUCTION, "auctionId", auctionId);
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        ToastUtil.success(btnBack, "❌ Đã huỷ phiên + hoàn cọc bidder");
                        loadMyAuctions();
                        loadMyItems(); // refresh cột status ở tab Sản phẩm
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
    private void handleRefreshAuctions() {
        loadMyAuctions();
    }

    private void addDeleteButtonToItemsTable() {
        colItemAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🗑️ Xoá");
            {
                btn.getStyleClass().add("btn-danger");
                btn.setOnAction(e -> {
                    Item it = getTableView().getItems().get(getIndex());
                    boolean ok = ModalDialog.confirm(
                            btn.getScene().getWindow(),
                            "Xác nhận xoá",
                            "Xoá sản phẩm \"" + it.getName() + "\" (#" + it.getId() + ")?\n\n"
                                    + "⚠️ Nếu sản phẩm đã có phiên đấu giá, server sẽ từ chối xoá.");
                    if (ok) deleteItem(it.getId());
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void deleteItem(int itemId) {
        new Thread(() -> {
            try {
                Response resp = conn.send(CommandType.DELETE_ITEM, "itemId", itemId);
                Platform.runLater(() -> {
                    if (resp.isSuccess()) {
                        ToastUtil.success(btnBack, "🗑 Đã xoá sản phẩm");
                        loadMyItems();
                    } else {
                        AlertUtil.showError("Lỗi", resp.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> AlertUtil.showError("Lỗi", e.getMessage()));
            }
        }).start();
    }

    private void clearItemForm() {
        if (txtItemName != null) txtItemName.clear();
        if (txtDescription != null) txtDescription.clear();
        if (txtStartingPrice != null) txtStartingPrice.clear();
        if (txtImageUrl != null) txtImageUrl.clear();
        if (txtExtraField1 != null) txtExtraField1.clear();
        if (txtExtraField2 != null) txtExtraField2.clear();
        if (txtExtraField3 != null) txtExtraField3.clear();
        if (txtExtraField4 != null) txtExtraField4.clear();
    }

    @FXML
    private void handleBack() {
        SceneManager.getInstance().switchScene("dashboard.fxml", "Dashboard",
                SceneManager.MAIN_WIDTH, SceneManager.MAIN_HEIGHT);
    }
}
