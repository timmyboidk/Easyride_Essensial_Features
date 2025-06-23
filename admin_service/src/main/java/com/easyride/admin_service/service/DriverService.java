package com.easyride.admin_service.service;

public interface DriverService {
    void updateVerificationStatus(Long driverId, String status);
}