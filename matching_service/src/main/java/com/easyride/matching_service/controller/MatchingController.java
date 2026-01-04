package com.easyride.matching_service.controller;

import com.easyride.matching_service.dto.*; // Ensure ApiResponse is here
// import com.easyride.common.dto.ApiResponse; // Or from common module
import com.easyride.matching_service.service.MatchingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/matching")
public class MatchingController {

    private static final Logger log = LoggerFactory.getLogger(MatchingController.class);
    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * Endpoint to manually trigger matching for a given order request.
     * This might be used for retries or specific scenarios.
     * Normal flow is event-driven via OrderEventListener.
     */
    @PostMapping("/matchDriver")
    public ApiResponse<DriverAssignedEventDto> matchDriver(@Valid @RequestBody MatchRequestDto matchRequestDto) {
        log.info("Received manual request to match driver for order: {}", matchRequestDto.getOrderId());
        // This now calls a refined matchDriver that returns an event DTO or similar
        DriverAssignedEventDto assignmentResult = matchingService.findAndAssignDriver(matchRequestDto);
        if (assignmentResult != null) {
            log.info("Manual match successful for order {}: driver {}", matchRequestDto.getOrderId(),
                    assignmentResult.getDriverId());
            return ApiResponse.success("司机匹配成功", assignmentResult);
        } else {
            log.warn("Manual match failed for order {}", matchRequestDto.getOrderId());
            return ApiResponse.error(404, "未能匹配到合适的司机");
        }
    }

    /**
     * Endpoint for drivers to update their status (location, availability, etc.)
     * Modified to accept a DTO.
     */
    @PostMapping("/driverStatus/{driverId}")
    public ApiResponse<String> updateDriverStatus(@PathVariable Long driverId,
            @Valid @RequestBody DriverStatusUpdateDto statusUpdateDto) {
        log.info("Received request to update status for driver {}: {}", driverId, statusUpdateDto);
        matchingService.updateDriverStatus(driverId, statusUpdateDto);
        log.info("Driver {} status updated successfully.", driverId);
        return ApiResponse.successMessage("司机状态更新成功");
    }

    /**
     * Endpoint for a driver to accept a manually offered order or a "grab-able"
     * order.
     * The orderId and driverId (from authenticated principal) are key.
     */
    @PostMapping("/orders/{orderId}/accept")
    public ApiResponse<String> acceptOrder(@PathVariable Long orderId /*
                                                                       * , @AuthenticationPrincipal CustomUserDetails
                                                                       * driverDetails
                                                                       */) {
        // Long driverId = driverDetails.getId(); // Get driver ID from authenticated
        // principal
        Long driverId = 1L; // Placeholder for authenticated driver
        log.info("Driver {} attempting to accept order {}", driverId, orderId);
        boolean accepted = matchingService.acceptOrder(orderId, driverId);
        if (accepted) {
            log.info("Order {} accepted by driver {}", orderId, driverId);
            return ApiResponse.successMessage("订单接受成功");
        } else {
            log.warn("Order {} could not be accepted by driver {} (e.g., already taken or invalid state)", orderId,
                    driverId);
            return ApiResponse.error(409, "订单无法接受（可能已被分配或状态无效）");
        }
    }

    /**
     * Endpoint for drivers to fetch a list of available orders for manual
     * selection/grabbing.
     */
    @GetMapping("/orders/available")
    public ApiResponse<List<AvailableOrderDto>> getAvailableOrders(/*
                                                                    * @RequestParam(required = false) String region,
                                                                    * etc.
                                                                    */) {
        // Potentially get driver's current region/preferences from their profile or
        // status
        log.info("Fetching available orders for manual selection.");
        List<AvailableOrderDto> availableOrders = matchingService.getAvailableOrdersForDriver(/*
                                                                                               * driverId,
                                                                                               * driverPreferences
                                                                                               */);
        return ApiResponse.success(availableOrders);
    }
}