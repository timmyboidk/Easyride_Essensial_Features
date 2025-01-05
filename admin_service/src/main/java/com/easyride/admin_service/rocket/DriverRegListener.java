package com.easyride.admin_service.rocket;

import com.easyride.admin_service.dto.DriverRegistrationEvent;
import com.easyride.admin_service.service.AdminService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RocketMQMessageListener(topic = "user-topic", consumerGroup = "admin-service-consumer-group")
public class DriverRegListener implements RocketMQListener<DriverRegistrationEvent> {

    private final AdminService adminService;

    public DriverRegListener(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void onMessage(DriverRegistrationEvent event) {
        log.info("[AdminService] Received DriverRegistrationEvent: {}", event);
        // 1. 解析信息，event中可能包含 driverId, driverName, licenseNumber 等
        // 2. 根据平台策略判断是否自动审核通过或需人工审核
        // 3. adminService 可写一方法，如 adminService.approveDriverRegistration(event)
    }
}
