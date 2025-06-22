package com.easyride.matching_service.service;

import com.easyride.matching_service.dto.AvailableOrderDto;
import com.easyride.matching_service.dto.DriverAssignedEventDto;
import com.easyride.matching_service.dto.DriverStatusUpdateDto;
import com.easyride.matching_service.dto.MatchRequestDto;
import java.util.List;

public interface MatchingService {

    /**
     * Finds the best-suited driver for a given match request, assigns the order,
     * and returns the assignment details.
     *
     * @param request The details of the match request.
     * @return A DTO containing details of the driver assignment, or null if no driver was found.
     */
    DriverAssignedEventDto findAndAssignDriver(MatchRequestDto request);

    /**
     * Updates a driver's status, including their availability and location.
     *
     * @param driverId        The ID of the driver to update.
     * @param statusUpdateDto A DTO containing the updated status information.
     */
    void updateDriverStatus(Long driverId, DriverStatusUpdateDto statusUpdateDto);

    /**
     * Makes an order available for drivers to see and "grab" or accept manually.
     *
     * @param matchRequest The details of the order to make available.
     */
    void makeOrderAvailableForGrabbing(MatchRequestDto matchRequest);

    /**
     * Retrieves a list of orders that are currently available for a driver to accept.
     *
     * @return A list of available orders.
     */
    List<AvailableOrderDto> getAvailableOrdersForDriver();

    /**
     * Allows a driver to accept or "grab" an available order.
     *
     * @param orderId  The ID of the order to accept.
     * @param driverId The ID of the driver accepting the order.
     * @return true if the order was successfully accepted, false otherwise.
     */
    boolean acceptOrder(Long orderId, Long driverId);
}