package com.easyride.admin_service.repository;

import com.easyride.admin_service.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    // 可以扩展一些查询方法，如按用户名查找
    AdminUser findByUsername(String username);
    boolean existsByUsername(String username);
}
