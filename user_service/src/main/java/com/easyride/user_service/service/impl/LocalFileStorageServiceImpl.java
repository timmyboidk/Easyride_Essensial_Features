package com.easyride.user_service.service.impl;

import com.easyride.user_service.exception.FileStorageException;
import com.easyride.user_service.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public LocalFileStorageServiceImpl() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                    ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new FileStorageException("Filename cannot be null");
        }

        if (originalFileName.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence " + originalFileName);
        }

        // 只取文件名部分，去除任何路径信息
        String cleanedFileName = Paths.get(originalFileName).getFileName().toString();
        // 生成唯一文件名
        String uniqueFileName = UUID.randomUUID().toString() + "_" + cleanedFileName;

        try {
            // 确保目标位置在存储目录内
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new FileStorageException("Filename contains invalid path sequence " + uniqueFileName);
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return targetLocation.toString();
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + uniqueFileName + ". Please try again!", ex);
        }
    }
}
