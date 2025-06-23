package com.easyride.admin_service.dto;

// Using a record for an immutable data carrier
public record LicenseInfo(String licenseNumber, String fullName, String expirationDate) {
}