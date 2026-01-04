package com.evaluation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_windows")
@Data
@NoArgsConstructor
// Optional helper entity if managing windows explicitly
public class ReviewWindow { // Tracks if a review can be submitted for an order
    @Id
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

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public LocalDateTime getWindowOpenTime() {
        return windowOpenTime;
    }

    public void setWindowOpenTime(LocalDateTime windowOpenTime) {
        this.windowOpenTime = windowOpenTime;
    }

    public LocalDateTime getWindowCloseTime() {
        return windowCloseTime;
    }

    public void setWindowCloseTime(LocalDateTime windowCloseTime) {
        this.windowCloseTime = windowCloseTime;
    }

    public boolean isPassengerCanReview() {
        return passengerCanReview;
    }

    public void setPassengerCanReview(boolean passengerCanReview) {
        this.passengerCanReview = passengerCanReview;
    }

    public boolean isDriverCanReview() {
        return driverCanReview;
    }

    public void setDriverCanReview(boolean driverCanReview) {
        this.driverCanReview = driverCanReview;
    }

    public boolean isPassengerReviewed() {
        return passengerReviewed;
    }

    public void setPassengerReviewed(boolean passengerReviewed) {
        this.passengerReviewed = passengerReviewed;
    }

    public boolean isDriverReviewed() {
        return driverReviewed;
    }

    public void setDriverReviewed(boolean driverReviewed) {
        this.driverReviewed = driverReviewed;
    }
}