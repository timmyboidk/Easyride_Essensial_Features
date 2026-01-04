package com.evaluation.service;

import java.time.LocalDateTime;

public interface ReviewWindowService {

    /**
     * Opens a new review window for a completed order.
     *
     * @param orderId       The ID of the order.
     * @param passengerId   The ID of the passenger.
     * @param driverId      The ID of the driver.
     * @param tripEndTime   The time the trip was completed.
     */
    void openReviewWindow(Long orderId, Long passengerId, Long driverId, LocalDateTime tripEndTime);
}