package com.evaluation.service;

import com.evaluation.model.ReviewWindow;
import com.evaluation.repository.ReviewWindowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReviewWindowServiceImpl implements ReviewWindowService {

    private static final Logger log = LoggerFactory.getLogger(ReviewWindowServiceImpl.class);

    private final ReviewWindowRepository reviewWindowRepository;

    public ReviewWindowServiceImpl(ReviewWindowRepository reviewWindowRepository) {
        this.reviewWindowRepository = reviewWindowRepository;
    }

    @Override
    @Transactional
    public void openReviewWindow(Long orderId, Long passengerId, Long driverId, LocalDateTime tripEndTime) {
        if (reviewWindowRepository.existsById(orderId)) {
            log.warn("Review window for order ID {} already exists. Ignoring event.", orderId);
            return;
        }

        ReviewWindow reviewWindow = new ReviewWindow(orderId, passengerId, driverId, tripEndTime);
        reviewWindowRepository.save(reviewWindow);

        log.info("Successfully opened review window for order ID: {}", orderId);
    }
}