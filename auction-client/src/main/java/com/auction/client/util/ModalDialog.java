package com.auction.client.util;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Custom modal dialog — thay AlertUtil.showConfirm + showInput bằng UI đẹp hơn.
 * - Modal Stage UNDECORATED, semi-transparent overlay.
 * - Centered trên owner window.
 * - Block FX thread như Alert.showAndWait — caller dùng giống pattern cũ.
 */
public final class ModalDialog {

    private ModalDialog() {}

    /** Confirm dialog: trả về true nếu user bấm OK. */
    public static boolean confirm(Window owner, String title, String message) {
        Stage stage = createBaseStage(owner);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("modal-title");

        Label bodyLbl = new Label(message);
        bodyLbl.getStyleClass().add("modal-body");
        bodyLbl.setWrapText(true);

        Button btnCancel = new Button("Hủy");
        btnCancel.getStyleClass().add("btn-secondary");
        Button btnOk = new Button("Đồng ý");
        btnOk.getStyleClass().add("btn-primary");

        boolean[] result = {false};
        btnOk.setOnAction(e -> { result[0] = true; stage.close(); });
        btnCancel.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, btnCancel, btnOk);

        VBox modal = new VBox(16, titleLbl, bodyLbl, buttons);
        modal.getStyleClass().add("modal-window");
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setMaxWidth(Region.USE_PREF_SIZE);

        showStage(stage, modal, owner);
        return result[0];
    }

    /** Modal thông báo lỗi — chỉ có 1 nút "Đã hiểu". */
    public static void error(Window owner, String title, String message) {
        notify(owner, "❌  " + title, message, "modal-title-danger");
    }

    /** Modal cảnh báo — chỉ có 1 nút. */
    public static void warning(Window owner, String title, String message) {
        notify(owner, "⚠️  " + title, message, "modal-title-warning");
    }

    /** Modal thông báo info — chỉ có 1 nút. */
    public static void info(Window owner, String title, String message) {
        notify(owner, "ℹ️  " + title, message, "modal-title-info");
    }

    private static void notify(Window owner, String title, String message, String titleClass) {
        // Wrap Platform.runLater để có thể gọi từ background thread (giống AlertUtil cũ).
        Runnable r = () -> {
            Stage stage = createBaseStage(owner);

            Label titleLbl = new Label(title);
            titleLbl.getStyleClass().addAll("modal-title", titleClass);

            Label bodyLbl = new Label(message);
            bodyLbl.getStyleClass().add("modal-body");
            bodyLbl.setWrapText(true);

            Button btnOk = new Button("Đã hiểu");
            btnOk.getStyleClass().add("btn-primary");
            btnOk.setOnAction(e -> stage.close());
            btnOk.setDefaultButton(true);

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            HBox buttons = new HBox(10, spacer, btnOk);

            VBox modal = new VBox(14, titleLbl, bodyLbl, buttons);
            modal.getStyleClass().add("modal-window");
            modal.setMaxHeight(Region.USE_PREF_SIZE);
            modal.setMaxWidth(Region.USE_PREF_SIZE);

            showStage(stage, modal, owner);
        };
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    /** Input dialog: trả về Optional<String> (empty nếu user hủy). */
    public static Optional<String> input(Window owner, String title, String message, String defaultValue) {
        Stage stage = createBaseStage(owner);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("modal-title");

        Label bodyLbl = new Label(message);
        bodyLbl.getStyleClass().add("modal-body");
        bodyLbl.setWrapText(true);

        TextField input = new TextField(defaultValue == null ? "" : defaultValue);
        input.getStyleClass().add("form-input");

        Button btnCancel = new Button("Hủy");
        btnCancel.getStyleClass().add("btn-secondary");
        Button btnOk = new Button("Xác nhận");
        btnOk.getStyleClass().add("btn-primary");

        String[] result = {null};
        btnOk.setOnAction(e -> { result[0] = input.getText(); stage.close(); });
        btnCancel.setOnAction(e -> stage.close());
        input.setOnAction(e -> btnOk.fire()); // Enter key submit

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox buttons = new HBox(10, spacer, btnCancel, btnOk);

        VBox modal = new VBox(14, titleLbl, bodyLbl, input, buttons);
        modal.getStyleClass().add("modal-window");
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setMaxWidth(Region.USE_PREF_SIZE);

        showStage(stage, modal, owner);
        return Optional.ofNullable(result[0]);
    }

    /** Helper tạo Stage cơ bản, undecorated + transparent. */
    private static Stage createBaseStage(Window owner) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);
        return stage;
    }

    /** Wrap modal trong StackPane overlay đen mờ, hiển thị block. */
    private static void showStage(Stage stage, VBox modal, Window owner) {
        StackPane overlay = new StackPane(modal);
        overlay.getStyleClass().add("modal-overlay");
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(40));

        Scene scene = new Scene(overlay);
        scene.setFill(null);
        // Load CSS
        java.net.URL css = ModalDialog.class.getResource("/css/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setScene(scene);

        // Match owner window size để overlay cover toàn bộ owner.
        // Nếu owner null (vd lỗi connect trước khi login → primaryStage chưa show), dùng default center.
        if (owner != null && owner.getWidth() > 0) {
            stage.setX(owner.getX());
            stage.setY(owner.getY());
            stage.setWidth(owner.getWidth());
            stage.setHeight(owner.getHeight());
        } else {
            stage.centerOnScreen();
        }

        stage.showAndWait();
    }
}
