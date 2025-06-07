package com.easyride.admin_service.controller;

import com.easyride.admin_service.dto.AdminDriverActionDto; // New DTO for approval/rejection payload
import com.easyride.admin_service.dto.ApiResponse;
import com.easyride.admin_service.dto.DriverApplicationDto;
import com.easyride.admin_service.service.AdminDriverManagementService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/drivers/applications")
// @PreAuthorize("hasAnyRole('ADMIN_DRIVER_VERIFICATION', 'SUPER_ADMIN')")
public class AdminDriverManagementController {
    private static final Logger log = LoggerFactory.getLogger(AdminDriverManagementController.class);

    private final AdminDriverManagementService driverManagementService;

    @Value("${easyride.admin.default-page-size:20}")
    private int defaultPageSize;

    @Autowired
    public AdminDriverManagementController(AdminDriverManagementService driverManagementService) {
        this.driverManagementService = driverManagementService;
    }

    @GetMapping("/pending")
    public ApiResponse<Page<DriverApplicationDto>> getPendingApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int pageSize = (size == null || size <= 0) ? defaultPageSize : size;
        log.info("Admin request to get pending driver applications: page={}, size={}", page, pageSize);
        Page<DriverApplicationDto> pendingApps = driverManagementService.getPendingDriverApplications(page, pageSize);
        return ApiResponse.success(pendingApps);
    }

    @GetMapping("/{driverId}")
    public ApiResponse<DriverApplicationDto> getApplicationDetails(@PathVariable Long driverId) {
        log.info("Admin request for driver application details: driverId={}", driverId);
        DriverApplicationDto appDetails = driverManagementService.getDriverApplicationDetails(driverId);
        return ApiResponse.success(appDetails);
    }

    @PostMapping("/{driverId}/approve")
    public ApiResponse<String> approveApplication(@PathVariable Long driverId, @Valid @RequestBody AdminDriverActionDto actionDto /*, @AuthenticationPrincipal UserPrincipal adminPrincipal */) {
        // Long adminId = adminPrincipal.getId();
        Long adminId = 0L; // Placeholder
        log.info("Admin {} approving driver application for ID: {}", adminId, driverId);
        driverManagementService.approveDriverApplication(driverId, adminId, actionDto.getNotes());
        return ApiResponse.successMessage("司机申请已批准");
    }

    @PostMapping("/{driverId}/reject")
    public ApiResponse<String> rejectApplication(@PathVariable Long driverId, @Valid @RequestBody AdminDriverActionDto actionDto /*, @AuthenticationPrincipal UserPrincipal adminPrincipal */) {
        // Long adminId = adminPrincipal.getId();
        Long adminId = 0L; // Placeholder
        log.info("Admin {} rejecting driver application for ID {} with reason: {}", adminId, driverId, actionDto.getReason());
        driverManagementService.rejectDriverApplication(driverId, adminId, actionDto.getReason(), actionDto.getNotes());
        return ApiResponse.successMessage("司机申请已拒绝");
    }
}