package com.easyride.admin_service.service;

import com.easyride.admin_service.client.UserServiceClient;
import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.exception.ResourceNotFoundException; // Create this
import com.easyride.admin_service.model.DriverApplication;
import com.easyride.admin_service.model.DriverApplicationStatus;
import com.easyride.admin_service.repository.DriverApplicationRepository;
import com.easyride.admin_service.service.DriverVerificationService; // Existing skeleton
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminDriverManagementServiceImpl implements AdminDriverManagementService {
    private static final Logger log = LoggerFactory.getLogger(AdminDriverManagementServiceImpl.class);

    private final DriverApplicationRepository applicationRepository;
    private final UserServiceClient userServiceClient; // To update status in User Service
    private final DriverVerificationService driverVerificationService; // For background checks etc.

    @Autowired
    public AdminDriverManagementServiceImpl(DriverApplicationRepository applicationRepository,
                                            UserServiceClient userServiceClient,
                                            DriverVerificationService driverVerificationService) {
        this.applicationRepository = applicationRepository;
        this.userServiceClient = userServiceClient;
        this.driverVerificationService = driverVerificationService;
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
        // app.setDriverLicenseDocumentUrl(event.getDriverLicenseDocumentUrl()); // if provided
        applicationRepository.save(app);
        log.info("New driver application for {} stored with PENDING_REVIEW status.", event.getDriverId());

        // Optionally trigger initial verification steps (e.g. OCR)
        // driverVerificationService.initiateDocumentVerification(app.getDriverId(), app.getDriverLicenseDocumentUrl());
    }

    @Override
    public Page<DriverApplicationDto> getPendingDriverApplications(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("applicationTime").ascending());
        Page<DriverApplication> applications = applicationRepository.findByStatus(DriverApplicationStatus.PENDING_REVIEW, pageable);
        return applications.map(this::mapToDto);
    }

    @Override
    public DriverApplicationDto getDriverApplicationDetails(Long driverId) {
        DriverApplication app = applicationRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver application not found for ID: " + driverId));
        return mapToDto(app);
    }

    @Override
    @Transactional
    public void approveDriverApplication(Long driverId, Long adminId, String notes) {
        log.info("Admin {} approving driver application for ID: {}", adminId, driverId);
        DriverApplication app = applicationRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver application not found: " + driverId));

        if (app.getStatus() != DriverApplicationStatus.PENDING_REVIEW) {
            log.warn("Driver application {} is not in PENDING_REVIEW state. Current status: {}", driverId, app.getStatus());
            throw new IllegalStateException("申请状态不正确，无法批准。");
        }

        // Perform final verification steps if any (e.g., background check)
        // boolean backgroundCheckOk = driverVerificationService.verifyBackground(driverId);
        // if (!backgroundCheckOk) {
        //     rejectDriverApplication(driverId, adminId, "背景审查未通过", notes);
        //     return;
        // }

        app.setStatus(DriverApplicationStatus.APPROVED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        app.setAdminNotes(notes);
        applicationRepository.save(app);

        // Notify User Service to update driver's main status/role to fully active
        AdminDriverUpdateDto userUpdate = new AdminDriverUpdateDto();
        userUpdate.setApprovalStatus("APPROVED"); // User Service should map this to its DriverApprovalStatus enum
        userUpdate.setEnabled(true);
        // userUpdate.setVehicleVerified(true); // Example
        try {
            ApiResponse<DriverDetailDto_FromUserService> userServiceResponse = userServiceClient.updateDriverDetailsByAdmin(driverId, userUpdate);
            if (userServiceResponse == null || userServiceResponse.getCode() != 0) {
                log.error("Failed to update driver status in User Service for {}. Feign response: {}", driverId, userServiceResponse);
                // Potentially mark for retry or manual reconciliation
                throw new ExternalServiceException("用户服务中更新司机状态失败。");
            }
            log.info("Driver {} approved. Status updated in User Service.", driverId);
            // TODO: Send a DRIVER_ACCOUNT_ACTIVATED notification event
        } catch (Exception e) {
            log.error("Exception calling User Service to update driver {}: {}", driverId, e.getMessage(), e);
            // Critical: application approved here, but User service update failed. Needs reconciliation.
            throw new ExternalServiceException("调用用户服务时出错：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void rejectDriverApplication(Long driverId, Long adminId, String reason, String notes) {
        // ... similar logic to approve, but set status to REJECTED and notify User Service ...
        log.info("Admin {} rejecting driver application for ID {} with reason: {}", adminId, driverId, reason);
        DriverApplication app = applicationRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver application not found: " + driverId));

        app.setStatus(DriverApplicationStatus.REJECTED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        app.setAdminNotes("拒绝原因: " + reason + ". " + notes);
        applicationRepository.save(app);

        AdminDriverUpdateDto userUpdate = new AdminDriverUpdateDto();
        userUpdate.setApprovalStatus("REJECTED");
        userUpdate.setEnabled(false); // Or keep enabled but with a rejected status
        userServiceClient.updateDriverDetailsByAdmin(driverId, userUpdate); // Handle response
        log.info("Driver {} application rejected.", driverId);
        // TODO: Send a DRIVER_APPLICATION_REJECTED notification event
    }

    private DriverApplicationDto mapToDto(DriverApplication app) {
        // Implement mapping from DriverApplication entity to DriverApplicationDto
        // This DTO would be returned by the controller
        DriverApplicationDto dto = new DriverApplicationDto();
        dto.setDriverId(app.getDriverId());
        dto.setUsername(app.getUsername());
        dto.setDriverLicenseNumber(app.getDriverLicenseNumber());
        dto.setStatus(app.getStatus().name());
        dto.setApplicationTime(app.getApplicationTime());
        dto.setAdminNotes(app.getAdminNotes());
        // Map other fields
        return dto;
    }
}