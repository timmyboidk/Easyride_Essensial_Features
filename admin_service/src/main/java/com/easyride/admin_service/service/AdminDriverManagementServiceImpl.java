package com.easyride.admin_service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.exception.ResourceNotFoundException;
import com.easyride.admin_service.model.DriverApplication;
import com.easyride.admin_service.model.DriverApplicationStatus;
import com.easyride.admin_service.repository.DriverApplicationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.easyride.admin_service.dto.DriverApplicationReviewedEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.easyride.admin_service.dto.DriverApplicationEventDto_Consumed;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminDriverManagementServiceImpl implements AdminDriverManagementService {
    private static final Logger log = LoggerFactory.getLogger(AdminDriverManagementServiceImpl.class);

    private final DriverApplicationMapper applicationMapper;
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.driver-review}")
    private String driverReviewTopic;

    @Value("${service-urls.user-service}")
    private String userServiceBaseUrl;

    public AdminDriverManagementServiceImpl(DriverApplicationMapper applicationMapper,
            RocketMQTemplate rocketMQTemplate) {
        this.applicationMapper = applicationMapper;
        this.rocketMQTemplate = rocketMQTemplate;
    }

    @Override
    @Transactional
    public void processNewDriverApplication(DriverApplicationEventDto_Consumed event) {
        log.info("Processing new driver application for driver ID: {}", event.getDriverId());
        if (applicationMapper.selectById(event.getDriverId()) != null) {
            log.warn("Driver application for ID {} already exists. Ignoring duplicate event.", event.getDriverId());
            return;
        }
        DriverApplication app = new DriverApplication(
                event.getDriverId(),
                event.getUsername(),
                event.getDriverLicenseNumber(),
                event.getApplicationTime() != null ? event.getApplicationTime() : LocalDateTime.now());
        applicationMapper.insert(app);
        log.info("New driver application for {} stored with PENDING_REVIEW status.", event.getDriverId());
    }

    @Override
    public org.springframework.data.domain.Page<DriverApplicationDto> getPendingDriverApplications(int page, int size) {
        Page<DriverApplication> mybatisPage = new Page<>(page + 1, size); // MyBatis-Plus is 1-indexed for pageNum?
                                                                          // Usually 1. Spring is 0.
        // Actually BaseMapper.selectPage expects a logical page object.
        // Default Page is 1-based. Spring Data is 0-based.

        LambdaQueryWrapper<DriverApplication> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DriverApplication::getStatus, DriverApplicationStatus.PENDING_REVIEW)
                .orderByAsc(DriverApplication::getApplicationTime);

        Page<DriverApplication> resultPage = applicationMapper.selectPage(mybatisPage, queryWrapper);

        List<DriverApplicationDto> dtos = resultPage.getRecords().stream()
                .map(this::mapToDto)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(page, size), resultPage.getTotal());
    }

    @Override
    public DriverApplicationDto getDriverApplicationDetails(Long driverId) {
        DriverApplication app = findApplicationById(driverId);
        return mapToDto(app);
    }

    @Override
    @Transactional
    public void approveDriverApplication(Long driverId, Long adminId, String notes) {
        log.info("Admin {} attempting to approve driver application for ID: {}", adminId, driverId);
        DriverApplication app = findApplicationById(driverId);

        if (app.getStatus() != DriverApplicationStatus.PENDING_REVIEW) {
            log.warn("Driver application {} is not in PENDING_REVIEW state. Current status: {}", driverId,
                    app.getStatus());
            throw new IllegalStateException("Application cannot be approved as it is not pending review.");
        }

        // 1. 更新本地数据库状态
        app.setStatus(DriverApplicationStatus.APPROVED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        app.setAdminNotes(notes);
        applicationMapper.updateById(app);
        log.info("Driver application {} approved locally.", driverId);

        // 2. 创建并发送消息到 MQ，通知 user_service
        DriverApplicationReviewedEvent event = new DriverApplicationReviewedEvent(
                driverId,
                "APPROVED",
                notes);
        rocketMQTemplate.convertAndSend(driverReviewTopic, event);
        log.info("Sent DRIVER_APPLICATION_REVIEWED (APPROVED) event for driver {}", driverId);
    }

    @Override
    @Transactional
    public void rejectDriverApplication(Long driverId, Long adminId, String reason, String notes) {
        log.info("Admin {} attempting to reject driver application for ID {} with reason: {}", adminId, driverId,
                reason);
        DriverApplication app = findApplicationById(driverId);

        // 1. 更新本地数据库状态
        app.setStatus(DriverApplicationStatus.REJECTED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        String fullNotes = "Reason: " + reason + ". Notes: " + notes;
        app.setAdminNotes(fullNotes);
        applicationMapper.updateById(app);
        log.info("Driver application {} rejected locally.", driverId);

        // 2. 创建并发送消息到 MQ，通知 user_service
        DriverApplicationReviewedEvent event = new DriverApplicationReviewedEvent(
                driverId,
                "REJECTED",
                fullNotes);
        rocketMQTemplate.convertAndSend(driverReviewTopic, event);
        log.info("Sent DRIVER_APPLICATION_REVIEWED (REJECTED) event for driver {}", driverId);
    }

    // `updateDriverStatusInUserService` 方法已不再需要，可以安全删除
    /*
     * private void updateDriverStatusInUserService(Long driverId,
     * AdminDriverUpdateDto updateDto, String action) {
     * ...
     * }
     */

    private DriverApplication findApplicationById(Long driverId) {
        DriverApplication app = applicationMapper.selectById(driverId);
        if (app == null) {
            throw new ResourceNotFoundException("Driver application not found for ID: " + driverId);
        }
        return app;
    }

    private DriverApplicationDto mapToDto(DriverApplication app) {
        DriverApplicationDto dto = new DriverApplicationDto();
        dto.setDriverId(app.getDriverId());
        dto.setUsername(app.getUsername());
        dto.setDriverLicenseNumber(app.getDriverLicenseNumber());
        dto.setStatus(app.getStatus().name());
        dto.setApplicationTime(app.getApplicationTime());
        dto.setAdminNotes(app.getAdminNotes());
        return dto;
    }

}