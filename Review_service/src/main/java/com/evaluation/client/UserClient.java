package com.evaluation.client;

import com.evaluation.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign 客户端接口，用于调用用户服务获取用户信息
 */
@FeignClient(name = "user-service")
public interface UserClient {

    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户数据传输对象
     */
    @GetMapping("/api/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
}
