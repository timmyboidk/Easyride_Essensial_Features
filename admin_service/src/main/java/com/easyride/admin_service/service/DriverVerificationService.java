package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.BackgroundCheckResult;
import com.easyride.admin_service.dto.DriverVerificationResult;
import com.easyride.admin_service.dto.LicenseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DriverVerificationService {

    // Added missing service fields
    @Autowired
    private OcrService ocrService;
    @Autowired
    private BackgroundCheckService backgroundCheckService;
    @Autowired
    private DriverService driverService;


    public DriverVerificationResult verifyDriver(Long driverId, MultipartFile licenseFile) {
        // 1. OCR 识别驾驶证
        LicenseInfo licenseInfo = ocrService.extractLicenseInfo(licenseFile);

        // 2. 背景检查
        BackgroundCheckResult checkResult = backgroundCheckService.performCheck(driverId, licenseInfo);

        // 3. 更新司机状态
        driverService.updateVerificationStatus(driverId, checkResult.getStatus());

        return new DriverVerificationResult(driverId, checkResult.getStatus(), licenseInfo);
    }
}