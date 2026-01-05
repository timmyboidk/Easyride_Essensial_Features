package com.easyride.user_service.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("admins")
@Data
@NoArgsConstructor
public class Admin extends User {
    @TableId
    private Long id;

    public Admin(String username, String password, String email, String phoneNumber) {
        super(username, password, email, phoneNumber, Role.ADMIN);
    }
}