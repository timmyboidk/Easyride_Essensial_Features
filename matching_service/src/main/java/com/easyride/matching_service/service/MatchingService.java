package com.easyride.matching_service.service;

import com.easyride.matching_service.dto.MatchRequestDto;

public interface MatchingService {

    /**
     * 自动匹配逻辑：根据乘客位置、订单需求等计算并返回最合适的司机ID
     */
    Long matchDriver(MatchRequestDto request);

    /**
     * 更新司机状态信息，如位置、可用性等
     */
    void updateDriverStatus(Long driverId, double lat, double lon, boolean available, double rating, String vehicleType);

    /**
     * 手动/自动接单处理
     * 例如，如果司机点击接受订单，可在这里更新状态
     */
    void makeOrderAvailableForGrabbing(MatchRequestDto matchRequest);
    List<AvailableOrderDto> getAvailableOrdersForDriver(/* Long driverId, DriverPreferences preferences */);
    boolean acceptOrder(Long orderId, Long driverId); // For grabbing
    // ... (inside MatchingService interface)
    void updateDriverStatus(Long driverId, DriverStatusUpdateDto statusUpdateDto);
}
