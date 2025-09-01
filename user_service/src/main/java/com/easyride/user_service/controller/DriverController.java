package com.easyride.user_service.controller;

import com.easyride.user_service.dto.ApiResponse;
import com.easyride.user_service.dto.DriverProfileUpdateDto;
import com.easyride.user_service.model.Driver;
import com.easyride.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/drivers") // 使用 /api/internal 路径前缀，表明这是内部服务调用的接口
public class DriverController {

    @Autowired
    private UserService userService;

    /**
     * 更新司机的审核状态和备注
     * 这是一个内部接口，主要由 admin-service 调用
     *
     * @param driverId 司机ID
     * @param updateDto 更新请求体
     * @return 包含更新后司机信息的标准响应
     */
    @PutMapping("/{driverId}/profile")
    public ResponseEntity<ApiResponse<Driver>> updateDriverProfile(
            @PathVariable Long driverId,
            @RequestBody DriverProfileUpdateDto updateDto) {

        Driver updatedDriver = userService.updateDriverProfile(driverId, updateDto);

        // 使用统一的 ApiResponse 格式返回成功响应
        return ResponseEntity.ok(new ApiResponse<>(0, "Driver profile updated successfully.", updatedDriver));
    }
}