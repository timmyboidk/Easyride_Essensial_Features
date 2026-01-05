// User.java
package com.easyride.order_service.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@TableName("users")
@Data
public class User {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private Role role;
}
