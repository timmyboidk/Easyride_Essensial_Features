package com.easyride.matching_service.service;

import com.easyride.matching_service.dto.LocationRequestEvent;
import com.easyride.matching_service.dto.MatchRequestDto;
import com.easyride.matching_service.model.DriverStatus;
import com.easyride.matching_service.repository.DriverStatusRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Comparator;

@Slf4j
@Service
public class MatchingServiceImpl implements MatchingService {

    private final DriverStatusRepository driverStatusRepository;
    private final RocketMQTemplate rocketMQTemplate;

    public MatchingServiceImpl(DriverStatusRepository driverStatusRepository,
                               RocketMQTemplate rocketMQTemplate) {
        this.driverStatusRepository = driverStatusRepository;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    public Long matchDriver(MatchRequestDto request) {
        // 1. 如果需要获取乘客地址信息，可以异步请求 location_service
        // 生成 correlationId
        String correlationId = UUID.randomUUID().toString();

        LocationRequestEvent locReq = new LocationRequestEvent(correlationId,
                request.getStartLatitude(),
                request.getStartLongitude());

        log.info("[MatchingService] Sending LocationRequestEvent to location-topic, correlationId={}", correlationId);

        // 2. 通过 rocketMQTemplate 将请求发送到 location_service
        rocketMQTemplate.convertAndSend("location-request-topic", locReq);

        // (异步) 当 location_service 完成后会发 LocationResponseEvent 到 "location-response-topic"
        // 由 LocationResponseListener 接收.

        // 3. 进行自动匹配司机
        List<DriverStatus> driverList = driverStatusRepository.findAll();
        driverList.removeIf(d -> !d.isAvailable() || !request.getVehicleType().equalsIgnoreCase(d.getVehicleType()));
        if (driverList.isEmpty()) {
            log.warn("No available drivers for orderId = {}", request.getOrderId());
            return null;
        }

        driverList.sort(Comparator.comparingDouble(d ->
                computeDistance(request.getStartLatitude(), request.getStartLongitude(), d.getLatitude(), d.getLongitude())
                        - d.getRating() * 10
        ));
        DriverStatus best = driverList.get(0);
        log.info("Auto-match success. orderId={} bestDriverId={}", request.getOrderId(), best.getDriverId());
        return best.getDriverId();
    }

    @Override
    public void updateDriverStatus(Long driverId, double lat, double lon, boolean available, double rating, String vehicleType) {
        DriverStatus status = driverStatusRepository.findById(driverId)
                .orElse(DriverStatus.builder().driverId(driverId).build());
        status.setLatitude(lat);
        status.setLongitude(lon);
        status.setAvailable(available);
        status.setRating(rating);
        status.setVehicleType(vehicleType);
        status.setLastUpdateTime(LocalDateTime.now());
        driverStatusRepository.save(status);
    }

    @Override
    public void acceptOrder(Long orderId, Long driverId) {
        // 通知 order_service: driver accepted
        log.info("Driver {} accepted order {}", driverId, orderId);
        // rocketMQTemplate.convertAndSend("order-topic", new DriverAcceptedEvent(orderId, driverId));
        // or use FeignClient
    }

    private double computeDistance(double lat1, double lon1, double lat2, double lon2) {
        double dx = lon1 - lon2;
        double dy = lat1 - lat2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
