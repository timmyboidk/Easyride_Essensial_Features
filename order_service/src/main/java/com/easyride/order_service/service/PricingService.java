package com.easyride.order_service.service;

import com.easyride.order_service.dto.EstimatedPriceInfo;
import com.easyride.order_service.dto.FinalPriceInfo;
import com.easyride.order_service.dto.PricingContext;
import com.easyride.order_service.model.Order;
import java.time.LocalDateTime;

public interface PricingService {
    EstimatedPriceInfo calculateEstimatedPrice(PricingContext context);
    FinalPriceInfo calculateFinalPrice(Order order, PricingContext context); // Corrected signature
    double calculateCancellationFee(Order order, LocalDateTime cancellationTime);
}