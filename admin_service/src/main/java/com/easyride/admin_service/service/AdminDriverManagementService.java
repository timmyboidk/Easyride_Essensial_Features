package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.model.DriverApplication;
import org.springframework.data.domain.Page;

public interface AdminDriverManagementService {
    void processNewDriverApplication(DriverApplicationEventDto_Consumed event);
    Page<DriverApplicationDto> getPendingDriverApplications(int page, int size);
    DriverApplicationDto getDriverApplicationDetails(Long driverId);
    void approveDriverApplication(Long driverId, Long adminId, String notes);
    void rejectDriverApplication(Long driverId, Long adminId, String reason, String notes);
    // Potentially method to request more info
}