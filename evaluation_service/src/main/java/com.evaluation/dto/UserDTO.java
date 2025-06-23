package com.evaluation.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户数据传输对象，用于接收用户服务返回的用户信息
 */
@Data
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    // 其他用户相关字段

    /**
     * 全参构造函数
     *
     * @param id       用户ID
     * @param username 用户名
     * @param email    用户邮箱
     */
    public UserDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }
}
