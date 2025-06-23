package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.LicenseInfo;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {
    LicenseInfo extractLicenseInfo(MultipartFile licenseFile);
}