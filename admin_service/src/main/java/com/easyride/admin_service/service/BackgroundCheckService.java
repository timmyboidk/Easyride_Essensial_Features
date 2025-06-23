package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.BackgroundCheckResult;
import com.easyride.admin_service.dto.LicenseInfo;

public interface BackgroundCheckService {
    BackgroundCheckResult performCheck(Long driverId, LicenseInfo licenseInfo);
}