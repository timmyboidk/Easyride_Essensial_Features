package com.easyride.admin_service.client;

import com.easyride.admin_service.dto.*; // Common DTOs for communication
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;


// Name should be the application name of User Service
@FeignClient(name = "user-service", url = "${service-urls.user-service}")
public interface UserServiceClient {

    // Endpoint in User Service to get paged list of users (passengers/drivers)
    // User Service needs to implement this endpoint
    @GetMapping("/admin/list") // Example endpoint in User Service for admin
    ApiResponse<UserPageDto_FromUserService> getUsers(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(name = "role", required = false) String role, // PASSENGER, DRIVER
            @RequestParam(name = "searchTerm", required = false) String searchTerm);

    @GetMapping("/internal/{userId}") // Existing or new internal endpoint in User Service
    ApiResponse<UserDetailDto_FromUserService> getUserById(@PathVariable("userId") Long userId);

    // Endpoint in User Service to update user profile (admin initiated)
    @PutMapping("/admin/{userId}/profile")
    ApiResponse<UserDetailDto_FromUserService> updateUserProfileByAdmin(
            @PathVariable("userId") Long userId,
            @RequestBody AdminUserProfileUpdateDto updateDto);

    @PostMapping("/admin/{userId}/enable")
    ApiResponse<Void> enableUser(@PathVariable("userId") Long userId);

    @PostMapping("/admin/{userId}/disable")
    ApiResponse<Void> disableUser(@PathVariable("userId") Long userId);

    // Endpoint in User Service to update driver details (like approval status, vehicle info by admin)
    @PutMapping("/admin/drivers/{driverId}")
    ApiResponse<DriverDetailDto_FromUserService> updateDriverDetailsByAdmin(
            @PathVariable("driverId") Long driverId,
            @RequestBody AdminDriverUpdateDto driverUpdateDto
    );
}