package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.exception.ExternalServiceException;
import com.easyride.admin_service.exception.ResourceNotFoundException;
import com.easyride.admin_service.model.DriverApplication;
import com.easyride.admin_service.model.DriverApplicationStatus;
import com.easyride.admin_service.repository.DriverApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import com.easyride.admin_service.dto.DriverApplicationReviewedEvent;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.easyride.admin_service.dto.DriverApplicationEventDto_Consumed;


import java.time.LocalDateTime;

@Service
public class AdminDriverManagementServiceImpl implements AdminDriverManagementService {
    private static final Logger log = LoggerFactory.getLogger(AdminDriverManagementServiceImpl.class);

    private final DriverApplicationRepository applicationRepository;
    private final DriverVerificationService driverVerificationService;
//    private final RestTemplate restTemplate;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.driver-review}")
    private String driverReviewTopic;

    @Value("${service-urls.user-service}")
    private String userServiceBaseUrl;

    @Autowired
    public AdminDriverManagementServiceImpl(DriverApplicationRepository applicationRepository,
                                            DriverVerificationService driverVerificationService) {
        this.applicationRepository = applicationRepository;
        this.driverVerificationService = driverVerificationService;
//        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    public void processNewDriverApplication(DriverApplicationEventDto_Consumed event) {
        log.info("Processing new driver application for driver ID: {}", event.getDriverId());
        if (applicationRepository.existsById(event.getDriverId())) {
            log.warn("Driver application for ID {} already exists. Ignoring duplicate event.", event.getDriverId());
            return;
        }
        DriverApplication app = new DriverApplication(
                event.getDriverId(),
                event.getUsername(),
                event.getDriverLicenseNumber(),
                event.getApplicationTime() != null ? event.getApplicationTime() : LocalDateTime.now()
        );
        applicationRepository.save(app);
        log.info("New driver application for {} stored with PENDING_REVIEW status.", event.getDriverId());
    }

    @Override
    public Page<DriverApplicationDto> getPendingDriverApplications(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("applicationTime").ascending());
        Page<DriverApplication> applications = applicationRepository.findByStatus(DriverApplicationStatus.PENDING_REVIEW, pageable);
        return applications.map(this::mapToDto);
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
            log.warn("Driver application {} is not in PENDING_REVIEW state. Current status: {}", driverId, app.getStatus());
            throw new IllegalStateException("Application cannot be approved as it is not pending review.");
        }

        // 1. 更新本地数据库状态
        app.setStatus(DriverApplicationStatus.APPROVED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        app.setAdminNotes(notes);
        applicationRepository.save(app);
        log.info("Driver application {} approved locally.", driverId);


        // 2. 创建并发送消息到 MQ，通知 user_service
        DriverApplicationReviewedEvent event = new DriverApplicationReviewedEvent(
                driverId,
                "APPROVED",
                notes
        );
        rocketMQTemplate.convertAndSend(driverReviewTopic, event);
        log.info("Sent DRIVER_APPLICATION_REVIEWED (APPROVED) event for driver {}", driverId);
    }

    @Override
    @Transactional
    public void rejectDriverApplication(Long driverId, Long adminId, String reason, String notes) {
        log.info("Admin {} attempting to reject driver application for ID {} with reason: {}", adminId, driverId, reason);
        DriverApplication app = findApplicationById(driverId);

        // 1. 更新本地数据库状态
        app.setStatus(DriverApplicationStatus.REJECTED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        String fullNotes = "Reason: " + reason + ". Notes: " + notes;
        app.setAdminNotes(fullNotes);
        applicationRepository.save(app);
        log.info("Driver application {} rejected locally.", driverId);

        // 2. 创建并发送消息到 MQ，通知 user_service
        DriverApplicationReviewedEvent event = new DriverApplicationReviewedEvent(
                driverId,
                "REJECTED",
                fullNotes
        );
        rocketMQTemplate.convertAndSend(driverReviewTopic, event);
        log.info("Sent DRIVER_APPLICATION_REVIEWED (REJECTED) event for driver {}", driverId);
    }

    // `updateDriverStatusInUserService` 方法已不再需要，可以安全删除
    /*
    private void updateDriverStatusInUserService(Long driverId, AdminDriverUpdateDto updateDto, String action) {
        ...
    }
    */


    private DriverApplication findApplicationById(Long driverId) {
        return applicationRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver application not found for ID: " + driverId));
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