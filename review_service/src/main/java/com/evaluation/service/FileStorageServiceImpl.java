package com.evaluation.service;

import com.evaluation.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务实现类，处理文件上传和存储
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    /**
     * 构造函数，初始化文件存储路径
     *
     * @param uploadDir 文件上传目录，可以通过配置文件指定
     */
    public FileStorageServiceImpl(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new BadRequestException("创建上传目录失败: " + uploadDir);
        }
    }

    /**
     * 存储多个文件到文件系统
     *
     * @param files 上传的文件数组
     * @return 存储后的文件路径列表
     */
    @Override
    public List<String> storeFiles(MultipartFile[] files) {
        List<String> filePaths = new ArrayList<>();
        for (MultipartFile file : files) {
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            // 生成唯一文件名，防止文件名冲突
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;
            try {
                // 检查文件名是否包含非法字符
                if (originalFileName.contains("..")) {
                    throw new BadRequestException("文件名包含非法路径序列: " + originalFileName);
                }

                // 复制文件到目标位置（覆盖已有文件）
                Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                // 添加文件路径到列表
                filePaths.add(targetLocation.toString());
            } catch (IOException ex) {
                throw new BadRequestException("存储文件失败: " + originalFileName);
            }
        }
        return filePaths;
    }
}
