package com.easyride.matching_service.controller;

import com.easyride.matching_service.dto.MatchRequestDto;
import com.easyride.matching_service.service.MatchingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/matching")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    /**
     * 当其他服务或前端需要让匹配服务进行“自动匹配”时，可调用此API
     */
    @PostMapping("/matchDriver")
    public Long matchDriver(@RequestBody MatchRequestDto dto) {
        return matchingService.matchDriver(dto);
    }

    /**
     * 司机状态更新接口，可供司机端或 Gateway 调用
     */
    @PostMapping("/driverStatus/{driverId}")
    public void updateDriverStatus(
            @PathVariable Long driverId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam boolean available,
            @RequestParam double rating,
            @RequestParam String vehicleType
    ) {
        matchingService.updateDriverStatus(driverId, latitude, longitude, available, rating, vehicleType);
    }

    /**
     * 当司机接受某订单时，可以由司机端或 Gateway 调用
     */
    @PostMapping("/acceptOrder")
    public void acceptOrder(@RequestParam Long orderId, @RequestParam Long driverId) {
        matchingService.acceptOrder(orderId, driverId);
    }
}
