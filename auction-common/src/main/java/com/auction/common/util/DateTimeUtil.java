package com.auction.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * DateTimeUtil — tiện ích xử lý thời gian.
 * Dùng chung cho cả Client và Server.
 * Chuẩn hóa format thời gian, tính khoảng cách, đếm ngược...
 */
public final class DateTimeUtil {

    public static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static final DateTimeFormatter DB_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateTimeUtil() {
    }

    /**
     * Lấy thời gian hiện tại.
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Format thời gian để hiển thị (dd/MM/yyyy HH:mm:ss).
     */
    public static String formatDisplay(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DISPLAY_FORMAT);
    }

    /**
     * Format thời gian để lưu DB (yyyy-MM-dd HH:mm:ss).
     */
    public static String formatDb(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DB_FORMAT);
    }

    /**
     * Parse thời gian từ DB string.
     */
    public static LocalDateTime parseDb(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        return LocalDateTime.parse(dateStr, DB_FORMAT);
    }

    /**
     * Parse thời gian từ ISO string.
     */
    public static LocalDateTime parseIso(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        return LocalDateTime.parse(dateStr, ISO_FORMAT);
    }

    /**
     * Tính số giây còn lại đến thời điểm target.
     * Trả về 0 nếu đã qua.
     */
    public static long secondsUntil(LocalDateTime target) {
        long seconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), target);
        return Math.max(0, seconds);
    }

    /**
     * Format số giây thành chuỗi đếm ngược HH:mm:ss.
     */
    public static String formatCountdown(long totalSeconds) {
        if (totalSeconds <= 0) return "00:00:00";
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Kiểm tra thời gian đã qua chưa.
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime != null && LocalDateTime.now().isAfter(dateTime);
    }

    /**
     * Kiểm tra thời gian có nằm trong khoảng không.
     */
    public static boolean isBetween(LocalDateTime target,
                                     LocalDateTime start, LocalDateTime end) {
        return target != null && !target.isBefore(start) && !target.isAfter(end);
    }
}
