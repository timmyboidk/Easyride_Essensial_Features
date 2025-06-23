package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.BackgroundCheckResult;
import com.easyride.admin_service.dto.LicenseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service // This annotation registers this class as a Spring bean
public class BackgroundCheckServiceImpl implements BackgroundCheckService {

    private static final Logger log = LoggerFactory.getLogger(BackgroundCheckServiceImpl.class);

    @Override
    public BackgroundCheckResult performCheck(Long driverId, LicenseInfo licenseInfo) {
        log.info("Performing mock background check for driver ID: {}", driverId);
        // Here you would call a third-party background check API.
        // We'll return a successful result for now.
        return new BackgroundCheckResult("APPROVED", "All checks passed.");
    }
}