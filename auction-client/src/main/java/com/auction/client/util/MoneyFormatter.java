package com.auction.client.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * MoneyFormatter — format/parse VNĐ với dấu phẩy phân cách hàng nghìn.
 * Ví dụ: 100000 → "100,000 VNĐ"; "100,000" → 100000.
 */
public final class MoneyFormatter {

    private static final DecimalFormatSymbols SYMBOLS;
    private static final DecimalFormat NUMBER_FORMAT;

    static {
        SYMBOLS = new DecimalFormatSymbols(Locale.US);
        SYMBOLS.setGroupingSeparator(',');
        NUMBER_FORMAT = new DecimalFormat("#,###", SYMBOLS);
    }

    private MoneyFormatter() {
    }

    /**
     * Format số tiền kèm hậu tố " VNĐ". Ví dụ: {@code 100000 → "100,000 VNĐ"}.
     */
    public static String format(double amount) {
        return NUMBER_FORMAT.format(amount) + " VNĐ";
    }

    /**
     * Format số tiền không có hậu tố (cho ô nhập). Ví dụ: {@code 100000 → "100,000"}.
     */
    public static String formatPlain(double amount) {
        return NUMBER_FORMAT.format(amount);
    }

    /**
     * Parse chuỗi "100,000" / "100,000 VNĐ" / "100000" về double.
     * Trả về 0 nếu chuỗi rỗng. Throws NumberFormatException nếu không parse được.
     */
    public static double parse(String text) {
        if (text == null) return 0;
        String cleaned = text.replace(",", "").replace(" ", "").replace("VNĐ", "").trim();
        if (cleaned.isEmpty()) return 0;
        return Double.parseDouble(cleaned);
    }

    /**
     * Gắn TextFormatter vào TextField để:
     * - Chỉ cho nhập chữ số và dấu phẩy.
     * - Tự thêm dấu phẩy phân cách hàng nghìn mỗi lần commit text.
     * Người gọi dùng {@link #parse(String)} để lấy giá trị số.
     */
    public static void attachTo(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            if (!change.isContentChange()) return change;
            String newText = change.getControlNewText().replace(",", "");
            if (newText.isEmpty()) return change;
            if (!newText.matches("\\d+")) return null;   // chỉ cho phép chữ số

            long value;
            try {
                value = Long.parseLong(newText);
            } catch (NumberFormatException e) {
                return null;
            }
            String formatted = NUMBER_FORMAT.format(value);

            change.setText(formatted);
            change.setRange(0, change.getControlText().length());
            change.setCaretPosition(formatted.length());
            change.setAnchor(formatted.length());
            return change;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }
}
