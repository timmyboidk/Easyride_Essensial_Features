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

import java.time.LocalDateTime;

@Service
public class AdminDriverManagementServiceImpl implements AdminDriverManagementService {
    private static final Logger log = LoggerFactory.getLogger(AdminDriverManagementServiceImpl.class);

    private final DriverApplicationRepository applicationRepository;
    private final DriverVerificationService driverVerificationService;
    private final RestTemplate restTemplate;

    @Value("${service-urls.user-service}")
    private String userServiceBaseUrl;

    @Autowired
    public AdminDriverManagementServiceImpl(DriverApplicationRepository applicationRepository,
                                            DriverVerificationService driverVerificationService,
                                            RestTemplate restTemplate) {
        this.applicationRepository = applicationRepository;
        this.driverVerificationService = driverVerificationService;
        this.restTemplate = restTemplate;
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

        // Here you could add calls to driverVerificationService if needed
        // boolean backgroundCheckOk = driverVerificationService.verifyBackground(driverId);
        // if (!backgroundCheckOk) { ... }

        // Update User Service BEFORE committing local transaction
        AdminDriverUpdateDto userUpdate = new AdminDriverUpdateDto();
        userUpdate.setApprovalStatus("APPROVED");
        userUpdate.setEnabled(true);
        updateDriverStatusInUserService(driverId, userUpdate, "approve");

        // If User Service update is successful, update local status
        app.setStatus(DriverApplicationStatus.APPROVED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        app.setAdminNotes(notes);
        applicationRepository.save(app);

        log.info("Driver application {} approved locally and in User Service.", driverId);
    }

    @Override
    @Transactional
    public void rejectDriverApplication(Long driverId, Long adminId, String reason, String notes) {
        log.info("Admin {} attempting to reject driver application for ID {} with reason: {}", adminId, driverId, reason);
        DriverApplication app = findApplicationById(driverId);

        // Update User Service BEFORE committing local transaction
        AdminDriverUpdateDto userUpdate = new AdminDriverUpdateDto();
        userUpdate.setApprovalStatus("REJECTED");
        userUpdate.setEnabled(false); // Or based on business rules
        updateDriverStatusInUserService(driverId, userUpdate, "reject");

        // If User Service update is successful, update local status
        app.setStatus(DriverApplicationStatus.REJECTED);
        app.setReviewedByAdminId(adminId);
        app.setReviewTime(LocalDateTime.now());
        app.setAdminNotes("Reason: " + reason + ". Notes: " + notes);
        applicationRepository.save(app);

        log.info("Driver application {} rejected locally and in User Service.", driverId);
    }

    private void updateDriverStatusInUserService(Long driverId, AdminDriverUpdateDto updateDto, String action) {
        String url = userServiceBaseUrl + "/admin/drivers/" + driverId;
        log.debug("Executing PUT request to {} to {} driver.", url, action);
        try {
            HttpEntity<AdminDriverUpdateDto> requestEntity = new HttpEntity<>(updateDto);
            ResponseEntity<ApiResponse<DriverDetailDto_FromUserService>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    new ParameterizedTypeReference<ApiResponse<DriverDetailDto_FromUserService>>() {}
            );

            ApiResponse<DriverDetailDto_FromUserService> apiResponse = responseEntity.getBody();

            if (responseEntity.getStatusCode() != HttpStatus.OK || apiResponse == null) {
                throw new ExternalServiceException(String.format("Failed to %s driver in User Service: Invalid response from server. Status: %s", action, responseEntity.getStatusCode()));
            }
            if (apiResponse.getCode() != 0) {
                throw new ExternalServiceException(String.format("Failed to %s driver in User Service: %s", action, apiResponse.getMessage()));
            }
            log.info("Successfully updated driver {} status in User Service.", driverId);
        } catch (RestClientException e) {
            log.error("Error calling User Service to {} driver {}: {}", action, driverId, e.getMessage());
            // Throwing exception here will cause the @Transactional method to roll back
            throw new ExternalServiceException("Unable to connect to User Service to update driver status: " + e.getMessage());
        }
    }

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