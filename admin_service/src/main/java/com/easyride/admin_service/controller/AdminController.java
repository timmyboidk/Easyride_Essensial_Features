package com.easyride.admin_service.controller;

import com.easyride.admin_service.dto.AdminUserDto;
import com.easyride.admin_service.dto.AdminOrderInterveneEvent;
import com.easyride.admin_service.model.AdminUser;
import com.easyride.admin_service.service.AdminService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // 1. 创建管理员用户
    @PostMapping("/users")
    public AdminUser createAdminUser(@RequestBody AdminUserDto dto) {
        return adminService.createAdminUser(dto);
    }

    // 2. 更新管理员用户
    @PutMapping("/users")
    public AdminUser updateAdminUser(@RequestBody AdminUserDto dto) {
        return adminService.updateAdminUser(dto);
    }

    // 3. 禁用管理员用户
    @PostMapping("/users/{adminUserId}/disable")
    public void disableAdminUser(@PathVariable Long adminUserId) {
        adminService.disableAdminUser(adminUserId);
    }

    // 4. 手动干预订单
    @PostMapping("/orders/intervene")
    public void interveneOrder(@RequestBody AdminOrderInterveneEvent dto) {
        adminService.interveneOrder(dto);
    }

    // 可以扩展更多接口，如查询管理员列表、配置系统等
}
