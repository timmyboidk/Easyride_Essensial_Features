package com.evaluation.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件存储服务接口，定义文件上传相关的操作
 */
public interface FileStorageService {

    /**
     * 存储多个文件
     *
     * @param files 上传的文件数组
     * @return 存储后的文件路径列表
     */
    List<String> storeFiles(MultipartFile[] files);
}
