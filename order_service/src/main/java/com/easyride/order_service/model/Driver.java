package com.easyride.order_service.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("drivers")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Driver extends User {
    // id 和 name/username 字段已从父类 User 继承，此处无需重复定义
    // @Id
    // private Long id;
    // private String name;

    private VehicleType vehicleType;

    private boolean available;
}
