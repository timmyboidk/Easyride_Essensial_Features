package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.AdminUserDto;
import com.easyride.admin_service.dto.AdminOrderInterveneEvent;
import com.easyride.admin_service.model.AdminUser;

public interface AdminService {

    // 用户管理相关
    AdminUser createAdminUser(AdminUserDto dto);
    AdminUser updateAdminUser(AdminUserDto dto);
    void disableAdminUser(Long adminUserId);

    // 手动干预订单分配
    void interveneOrder(AdminOrderInterveneEvent dto);

    // 可以添加更多财务管理、系统配置等接口
}
