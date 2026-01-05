package com.easyride.user_service.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("passengers")
@Getter
@Setter
@NoArgsConstructor
public class Passenger extends User {
    @TableId
    private Long id;

    public Passenger(String username, String password, String email, String phoneNumber) {
        super(username, password, email, phoneNumber, Role.PASSENGER);
    }

}