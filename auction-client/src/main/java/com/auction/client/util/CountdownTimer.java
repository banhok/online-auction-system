package com.auction.client.util;

import com.auction.common.util.DateTimeUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * CountdownTimer — đồng hồ đếm ngược cho phiên đấu giá.
 * Cập nhật Label mỗi giây, đổi màu đỏ khi < 1 phút.
 */
public class CountdownTimer {

    private Timeline timeline;
    private final Label label;
    private LocalDateTime endTime;
    private Runnable onFinished;

    public CountdownTimer(Label label) {
        this.label = label;
    }

    /**
     * Bắt đầu đếm ngược.
     * @param endTime thời gian kết thúc phiên đấu giá
     * @param onFinished callback khi hết giờ
     */
    public void start(LocalDateTime endTime, Runnable onFinished) {
        this.endTime = endTime;
        this.onFinished = onFinished;

        stop(); // Dừng timer cũ nếu có

        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Tick ngay lập tức lần đầu
        tick();
    }

    /**
     * Cập nhật endTime (khi có anti-snipe extension).
     */
    public void updateEndTime(LocalDateTime newEndTime) {
        this.endTime = newEndTime;
    }

    /**
     * Mỗi giây cập nhật label.
     */
    private void tick() {
        if (endTime == null) return;

        long secondsLeft = DateTimeUtil.secondsUntil(endTime);

        Platform.runLater(() -> {
            if (secondsLeft <= 0) {
                label.setText("Đã kết thúc");
                label.getStyleClass().removeAll("countdown-warning", "countdown-normal");
                label.getStyleClass().add("countdown-ended");
                stop();
                if (onFinished != null) {
                    onFinished.run();
                }
            } else {
                label.setText(DateTimeUtil.formatCountdown(secondsLeft));

                // Đổi màu khi < 60 giây
                label.getStyleClass().removeAll("countdown-warning", "countdown-normal", "countdown-ended");
                if (secondsLeft <= 60) {
                    label.getStyleClass().add("countdown-warning");
                } else {
                    label.getStyleClass().add("countdown-normal");
                }
            }
        });
    }

    /**
     * Dừng timer.
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }
}
