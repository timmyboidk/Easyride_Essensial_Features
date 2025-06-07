package com.easyride.admin_service.controller;

import com.easyride.admin_service.dto.*;
import com.easyride.admin_service.service.AdminUserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/platform-users") // Differentiate from /admin/users which manages AdminUser entities
// @PreAuthorize("hasAnyRole('ADMIN_USER_MANAGEMENT', 'SUPER_ADMIN')")
public class AdminUserController {
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    @Value("${easyride.admin.default-page-size:20}")
    private int defaultPageSize;

    @Autowired
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<UserPageDto_FromUserService> listPlatformUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String role, // e.g., PASSENGER, DRIVER
            @RequestParam(required = false) String searchTerm) {
        int pageSize = (size == null || size <= 0) ? defaultPageSize : size;
        log.info("Request to list platform users: page={}, size={}, role={}, search={}", page, pageSize, role, searchTerm);
        UserPageDto_FromUserService usersPage = adminUserService.listUsers(page, pageSize, role, searchTerm);
        return ApiResponse.success(usersPage);
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserDetailDto_FromUserService> getPlatformUserDetails(@PathVariable Long userId) {
        log.info("Request to get details for platform user ID: {}", userId);
        UserDetailDto_FromUserService userDetails = adminUserService.getUserDetails(userId);
        return ApiResponse.success(userDetails);
    }

    @PutMapping("/{userId}/profile")
    public ApiResponse<UserDetailDto_FromUserService> updatePlatformUserProfile(@PathVariable Long userId, @Valid @RequestBody AdminUserProfileUpdateDto updateDto) {
        log.info("Request to update profile for platform user ID: {}", userId);
        UserDetailDto_FromUserService updatedUser = adminUserService.updateUserProfile(userId, updateDto);
        return ApiResponse.success("用户资料更新成功", updatedUser);
    }

    @PostMapping("/{userId}/enable")
    public ApiResponse<Void> enablePlatformUser(@PathVariable Long userId) {
        log.info("Request to enable platform user ID: {}", userId);
        adminUserService.enableUser(userId);
        return ApiResponse.successMessage("用户已启用");
    }

    @PostMapping("/{userId}/disable")
    public ApiResponse<Void> disablePlatformUser(@PathVariable Long userId) {
        log.info("Request to disable platform user ID: {}", userId);
        adminUserService.disableUser(userId);
        return ApiResponse.successMessage("用户已禁用");
    }
}