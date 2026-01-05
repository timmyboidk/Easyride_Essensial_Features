package com.evaluation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDateTime;

@TableName("review_windows")
@Data
@NoArgsConstructor
// Optional helper entity if managing windows explicitly
public class ReviewWindow { // Tracks if a review can be submitted for an order
    @TableId(type = IdType.INPUT)
    private Long orderId; // Use Order ID as primary key

    private Long passengerId;
    private Long driverId;

    private LocalDateTime windowOpenTime;
    private LocalDateTime windowCloseTime; // e.g., 7 days after trip

    private boolean passengerCanReview;
    private boolean driverCanReview;

    private boolean passengerReviewed;
    private boolean driverReviewed;

    public ReviewWindow(Long orderId, Long passengerId, Long driverId, LocalDateTime tripEndTime) {
        this.orderId = orderId;
        this.passengerId = passengerId;
        this.driverId = driverId;
        this.windowOpenTime = tripEndTime;
        this.windowCloseTime = tripEndTime.plusDays(7); // Example: 7-day window
        this.passengerCanReview = true;
        this.driverCanReview = true;
        this.passengerReviewed = false;
        this.driverReviewed = false;
    }
}