package com.easyride.order_service.service;

import com.easyride.order_service.dto.EstimatedPriceInfo;
import com.easyride.order_service.dto.FinalPriceInfo;
import com.easyride.order_service.dto.PricingContext;
import com.easyride.order_service.exception.PricingException;
import com.easyride.order_service.model.Order;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.util.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

//basic implemenation,subject to change in terms of business model
//inject PricingService into OrderServiceImpl and use it in createOrder and when calculating final cost.
@Service
public class PricingServiceImpl implements PricingService {

    private static final Logger log = LoggerFactory.getLogger(PricingServiceImpl.class);

    // Base rates (example, move to config or DB)
    private static final double BASE_FARE_NORMAL = 2.5; // USD
    private static final double PER_KM_RATE_NORMAL = 1.5;
    private static final double PER_MINUTE_RATE_NORMAL = 0.2;

    private static final double BASE_FARE_CARPOOL = 1.8;
    private static final double PER_KM_RATE_CARPOOL = 1.0;
    private static final double PER_MINUTE_RATE_CARPOOL = 0.15;

    private static final double AIRPORT_SURCHARGE = 5.0;
    private static final double CHARTER_HOURLY_RATE = 50.0;

    private static final double CANCELLATION_FEE_DRIVER_EN_ROUTE = 5.0; // Example fee
    private static final double CANCELLATION_FEE_ARRIVED = 8.0;
    private static final int FREE_CANCELLATION_WINDOW_MINUTES = 5; // After order acceptance

    @Override
    public EstimatedPriceInfo calculateEstimatedPrice(PricingContext context) {
        if (context.getStartLocation() == null
                || (context.getEndLocation() == null && context.getServiceType() != ServiceType.CHARTER_HOURLY)) {
            throw new PricingException("Start or end location missing for price estimation.");
        }
        log.info("Calculating estimated price for context: {}", context);

        double distanceKm = 0;
        double durationMinutes = 5;

        if (context.getEndLocation() != null) {
            distanceKm = DistanceCalculator.calculateDistance(
                    context.getStartLocation().getLatitude(), context.getStartLocation().getLongitude(),
                    context.getEndLocation().getLatitude(), context.getEndLocation().getLongitude());

            // Simplified duration estimation (e.g., average speed of 30km/h)
            durationMinutes = (distanceKm / 30.0) * 60.0;
            if (distanceKm == 0)
                durationMinutes = 5; // Min duration for very short trips
        }

        double estimatedCost;
        double baseFare = BASE_FARE_NORMAL;
        double perKmRate = PER_KM_RATE_NORMAL;
        double perMinuteRate = PER_MINUTE_RATE_NORMAL;

        StringBuilder breakdown = new StringBuilder();

        switch (context.getServiceType()) {
            case CARPOOL:
                baseFare = BASE_FARE_CARPOOL;
                perKmRate = PER_KM_RATE_CARPOOL;
                perMinuteRate = PER_MINUTE_RATE_CARPOOL;
                break;
            case AIRPORT_TRANSFER:
                baseFare += AIRPORT_SURCHARGE;
                break;
            case CHARTER_HOURLY:
                // For charter, estimation might be different, e.g. min hours
                if (context.getScheduledTime() != null && context.getEndLocation() == null) { // Assuming charter
                                                                                              // implies duration based
                    // Let's say user specifies hours, or we estimate a minimum
                    durationMinutes = context.getActualDurationMinutes() != null ? context.getActualDurationMinutes()
                            : 60.0; // default 1 hour
                    estimatedCost = CHARTER_HOURLY_RATE * (durationMinutes / 60.0);
                    breakdown.append(String.format("Charter Hourly Rate: $%.2f/hr, Estimated Hours: %.2f",
                            CHARTER_HOURLY_RATE, durationMinutes / 60.0));
                    return EstimatedPriceInfo.builder()
                            .estimatedCost(estimatedCost)
                            .estimatedDistance(0) // Distance might not be primary factor
                            .estimatedDuration(durationMinutes)
                            .currency("USD")
                            .priceBreakdown(breakdown.toString())
                            .build();
                } // else fall through to normal if start/end provided for some reason
                break;
            // Add cases for LONG_DISTANCE, EXPRESS, LUXURY with different
            // base/rates/multipliers
            default:
                // Use normal rates
                break;
        }

        estimatedCost = baseFare + (distanceKm * perKmRate) + (durationMinutes * perMinuteRate);

        // Peak hour surcharge (example: 7-9 AM and 5-7 PM, 20% surcharge)
        if (context.getScheduledTime() != null) {
            LocalTime scheduledTime = context.getScheduledTime().toLocalTime();
            if ((scheduledTime.isAfter(LocalTime.of(7, 0)) && scheduledTime.isBefore(LocalTime.of(9, 0))) ||
                    (scheduledTime.isAfter(LocalTime.of(17, 0)) && scheduledTime.isBefore(LocalTime.of(19, 0)))) {
                double surge = estimatedCost * 0.20;
                estimatedCost += surge;
                breakdown.append(String.format(
                        "Base: $%.2f, Distance (%.2fkm * $%.2f): $%.2f, Duration (%.2fmin * $%.2f): $%.2f, Peak Surge: $%.2f",
                        baseFare, distanceKm, perKmRate, distanceKm * perKmRate, durationMinutes, perMinuteRate,
                        durationMinutes * perMinuteRate, surge));
            } else {
                breakdown.append(String.format(
                        "Base: $%.2f, Distance (%.2fkm * $%.2f): $%.2f, Duration (%.2fmin * $%.2f): $%.2f",
                        baseFare, distanceKm, perKmRate, distanceKm * perKmRate, durationMinutes, perMinuteRate,
                        durationMinutes * perMinuteRate));
            }
        }

        log.info("Estimated price: cost={}, distance={}, duration={}", estimatedCost, distanceKm, durationMinutes);
        return EstimatedPriceInfo.builder()
                .estimatedCost(Math.max(estimatedCost, baseFare)) // Ensure minimum is base fare
                .estimatedDistance(distanceKm)
                .estimatedDuration(durationMinutes)
                .currency("USD") // Configurable
                .priceBreakdown(breakdown.toString())
                .build();
    }

    @Override
    public FinalPriceInfo calculateFinalPrice(Order order, PricingContext finalPricingContext) { // Corrected signature
        if (order.getDriverAssignedTime() == null || order.getOrderTime() == null) {
            throw new PricingException("Cannot calculate final price without trip times.");
        }

        LocalDateTime startTime = order.getDriverAssignedTime();
        LocalDateTime endTime = order.getOrderTime();
        long durationInMinutes = Duration.between(startTime, endTime).toMinutes();

        double distance = DistanceCalculator.calculateDistance(
                finalPricingContext.getStartLocation().getLatitude(),
                finalPricingContext.getStartLocation().getLongitude(),
                finalPricingContext.getEndLocation().getLatitude(),
                finalPricingContext.getEndLocation().getLongitude());

        long distanceCost = (long) (PER_KM_RATE_NORMAL * distance);
        long timeCost = (long) (PER_MINUTE_RATE_NORMAL * durationInMinutes);
        long finalPrice = (long) (BASE_FARE_NORMAL + distanceCost + timeCost);

        FinalPriceInfo finalPriceInfo = new FinalPriceInfo();
        finalPriceInfo.setFinalCost(finalPrice);
        finalPriceInfo.setActualDistance(distance);
        finalPriceInfo.setActualDuration(durationInMinutes);
        finalPriceInfo.setBaseFare((long) BASE_FARE_NORMAL);
        finalPriceInfo.setDistanceCost(distanceCost);
        finalPriceInfo.setTimeCost(timeCost);

        return finalPriceInfo;
    }

    // Implement calculateCancellationFee
    @Override
    public double calculateCancellationFee(Order order, LocalDateTime cancellationTime) {
        log.info("Calculating cancellation fee for order ID: {}", order.getId());
        double fee = 0.0;

        if (order.getStatus() == OrderStatus.SCHEDULED && order.getScheduledTime() != null) {
            // Rule for scheduled orders: e.g., free if cancelled > 1 hour before scheduled
            // time
            if (cancellationTime.isBefore(order.getScheduledTime().minusHours(1))) {
                return 0.0;
            } else {
                return BASE_FARE_NORMAL; // Example fixed fee for late scheduled cancellation
            }
        }

        // Rules for on-demand orders after driver assigned/accepted
        LocalDateTime decisionPointTime = order.getDriverAssignedTime(); // Add this field to Order model
        if (order.getStatus() == OrderStatus.DRIVER_ASSIGNED || order.getStatus() == OrderStatus.ACCEPTED) {
            decisionPointTime = order.getUpdatedAt(); // Time it was accepted/assigned
        } else if (order.getStatus() == OrderStatus.DRIVER_EN_ROUTE || order.getStatus() == OrderStatus.ARRIVED) {
            decisionPointTime = order.getDriverEnRouteTime() != null ? order.getDriverEnRouteTime()
                    : order.getUpdatedAt(); // Add driverEnRouteTime
        }

        if (decisionPointTime != null && Duration.between(decisionPointTime, cancellationTime)
                .toMinutes() > FREE_CANCELLATION_WINDOW_MINUTES) {
            switch (order.getStatus()) {
                case DRIVER_ASSIGNED: // or ACCEPTED
                case DRIVER_EN_ROUTE:
                    fee = CANCELLATION_FEE_DRIVER_EN_ROUTE;
                    log.info("Cancellation fee (driver en route) applied: ${}", fee);
                    break;
                case ARRIVED:
                    fee = CANCELLATION_FEE_ARRIVED;
                    log.info("Cancellation fee (driver arrived) applied: ${}", fee);
                    break;
                default:
                    fee = 0.0; // No fee if cancelled before driver is well on their way or too early
                    break;
            }
        } else {
            log.info("Order {} cancelled within free window or in a non-chargeable state.", order.getId());
        }
        return fee;
    }
}