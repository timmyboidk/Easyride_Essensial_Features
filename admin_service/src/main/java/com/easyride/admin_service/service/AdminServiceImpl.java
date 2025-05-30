package com.easyride.admin_service.service;

import com.easyride.admin_service.dto.AdminUserDto;
import com.easyride.admin_service.dto.AdminOrderInterveneEvent;
import com.easyride.admin_service.model.*;
import com.easyride.admin_service.repository.AdminUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.easyride.admin_service.model.AdminUser;
import com.easyride.admin_service.rocket.AdminRocketProducer;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AdminServiceImpl implements AdminService {

    private final AdminUserRepository adminUserRepository;
    // 假设我们还需要 OrderServiceClient 等远程调用或 RocketMQTemplate 来通知 order_service
    private final AdminRocketProducer adminRocketProducer;// 注入Producer

    public AdminServiceImpl(AdminUserRepository adminUserRepository,
                            AdminRocketProducer adminRocketProducer) {
        this.adminUserRepository = adminUserRepository;
        this.adminRocketProducer = adminRocketProducer;
    }

    @Override
    @Transactional
    public AdminUser createAdminUser(AdminUserDto dto) {
        if (adminUserRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("管理员用户名已存在");
        }
        AdminUser user = AdminUser.builder()
                .username(dto.getUsername())
                .password(dto.getPassword()) // 生产环境需加密
                .role(Role.valueOf(dto.getRole().toUpperCase()))
                .enabled(dto.isEnabled())
                .build();
        return adminUserRepository.save(user);
    }

    @Override
    @Transactional
    public AdminUser updateAdminUser(AdminUserDto dto) {
        AdminUser user = adminUserRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("管理员用户不存在"));
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRole(Role.valueOf(dto.getRole().toUpperCase()));
        user.setEnabled(dto.isEnabled());
        return adminUserRepository.save(user);
    }

    @Override
    @Transactional
    public void disableAdminUser(Long adminUserId) {
        AdminUser user = adminUserRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("管理员用户不存在"));
        user.setEnabled(false);
        adminUserRepository.save(user);
    }


    @Override
    public void interveneOrder(AdminOrderInterveneEvent dto) {
        // 1. 记录审计日志 / 验证订单合法性
        // 2. 发送事件给 order_service
        AdminOrderInterveneEvent event = AdminOrderInterveneEvent.builder()
                .orderId(dto.getOrderId())
                .action(dto.getAction())   // "REASSIGN" / "CANCEL"
                .reason(dto.getReason())
                .adminUserId(999L)  // 假设当前管理员ID
                .operateTime(LocalDateTime.now())
                .build();

        adminRocketProducer.sendOrderInterveneEvent(event);
    }
}
