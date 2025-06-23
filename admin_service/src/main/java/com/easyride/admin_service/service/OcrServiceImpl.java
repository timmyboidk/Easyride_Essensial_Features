package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.LicenseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service // This annotation tells Spring to create a bean of this class
public class OcrServiceImpl implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceImpl.class);

    @Override
    public LicenseInfo extractLicenseInfo(MultipartFile licenseFile) {
        log.info("Performing mock OCR on file: {}", licenseFile.getOriginalFilename());
        // In a real application, you would integrate with an OCR library or cloud service.
        // For now, we return dummy data.
        return new LicenseInfo("DUMMY-LICENSE-123", "John Doe", "2030-12-31");
    }
}