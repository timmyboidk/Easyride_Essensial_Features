package com.easyride.admin_service.dto;

public record DriverVerificationResult(Long driverId, String status, LicenseInfo licenseInfo) {
}