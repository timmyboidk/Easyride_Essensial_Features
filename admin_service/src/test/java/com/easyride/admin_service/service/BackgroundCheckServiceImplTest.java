package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.BackgroundCheckResult;
import com.easyride.admin_service.dto.LicenseInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BackgroundCheckServiceImplTest {

    private final BackgroundCheckServiceImpl service = new BackgroundCheckServiceImpl();

    @Test
    void performCheck_ReturnsApproved() {
        LicenseInfo licenseInfo = new LicenseInfo("123", "Name", "2030-01-01");
        // Set fields if needed, but implementation ignores them for now

        BackgroundCheckResult result = service.performCheck(1L, licenseInfo);

        assertNotNull(result);
        assertEquals("APPROVED", result.status());
        assertEquals("All checks passed.", result.details());
    }
}
