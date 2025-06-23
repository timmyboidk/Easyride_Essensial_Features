package com.easyride.admin_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service // And another bean for Spring to manage
public class DriverServiceImpl implements DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverServiceImpl.class);

    @Override
    public void updateVerificationStatus(Long driverId, String status) {
        // This method would typically update the driver's status in the database
        // or call another service (like the User Service) to do so.
        log.info("Updating verification status for driver ID {} to: {}", driverId, status);
        // For now, we just log the action.
    }
}