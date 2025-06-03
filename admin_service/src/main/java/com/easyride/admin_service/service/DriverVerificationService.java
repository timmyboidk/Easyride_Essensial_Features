package com.easyride.admin_service.service;

@Service
public class DriverVerificationService {

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
