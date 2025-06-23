package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.*; // DTOs used within Admin Service

public interface AdminUserService {
    UserPageDto_FromUserService listUsers(int page, int size, String role, String searchTerm);
    UserDetailDto_FromUserService getUserDetails(Long userId);
    UserDetailDto_FromUserService updateUserProfile(Long userId, AdminUserProfileUpdateDto updateDto);
    void enableUser(Long userId);
    void disableUser(Long userId);
}