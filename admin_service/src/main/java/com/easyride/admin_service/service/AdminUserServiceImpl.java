package com.easyride.admin_service.service;

import com.easyride.admin_service.client.UserServiceClient;
import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.exception.ExternalServiceException; // Create this
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private final UserServiceClient userServiceClient;

    @Autowired
    public AdminUserServiceImpl(UserServiceClient userServiceClient) {
        this.userServiceClient = userServiceClient;
    }

    @Override
    public UserPageDto_FromUserService listUsers(int page, int size, String role, String searchTerm) {
        log.info("Fetching users list: page={}, size={}, role={}, search={}", page, size, role, searchTerm);
        ApiResponse<UserPageDto_FromUserService> response = userServiceClient.getUsers(page, size, role, searchTerm);
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.error("Failed to fetch users from User Service. Response: {}", response);
            throw new ExternalServiceException("无法从用户服务获取用户列表: " + (response != null ? response.getMessage() : "无响应"));
        }
        return response.getData();
    }

    @Override
    public UserDetailDto_FromUserService getUserDetails(Long userId) {
        log.info("Fetching details for user ID: {}", userId);
        ApiResponse<UserDetailDto_FromUserService> response = userServiceClient.getUserById(userId);
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.error("Failed to fetch user details for ID {} from User Service. Response: {}", userId, response);
            throw new ExternalServiceException("无法获取用户详情: " + (response != null ? response.getMessage() : "无响应"));
        }
        return response.getData();
    }

    @Override
    public UserDetailDto_FromUserService updateUserProfile(Long userId, AdminUserProfileUpdateDto updateDto) {
        log.info("Admin updating profile for user ID: {}", userId);
        ApiResponse<UserDetailDto_FromUserService> response = userServiceClient.updateUserProfileByAdmin(userId, updateDto);
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            log.error("Failed to update profile for user ID {} via User Service. Response: {}", userId, response);
            throw new ExternalServiceException("更新用户资料失败: " + (response != null ? response.getMessage() : "无响应"));
        }
        return response.getData();
    }

    @Override
    public void enableUser(Long userId) {
        log.info("Admin enabling user ID: {}", userId);
        ApiResponse<Void> response = userServiceClient.enableUser(userId);
        if (response == null || response.getCode() != 0) {
            log.error("Failed to enable user ID {} via User Service. Response: {}", userId, response);
            throw new ExternalServiceException("启用用户失败: " + (response != null ? response.getMessage() : "无响应"));
        }
    }

    @Override
    public void disableUser(Long userId) {
        log.info("Admin disabling user ID: {}", userId);
        ApiResponse<Void> response = userServiceClient.disableUser(userId);
        if (response == null || response.getCode() != 0) {
            log.error("Failed to disable user ID {} via User Service. Response: {}", userId, response);
            throw new ExternalServiceException("禁用用户失败: " + (response != null ? response.getMessage() : "无响应"));
        }
    }
}