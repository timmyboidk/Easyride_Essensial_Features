package com.easyride.matching_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatus {

    @Id
    private Long driverId;          // 与 user_service 对应的司机ID

    private Double latitude;        // 司机当前位置
    private Double longitude;       // 司机当前位置
    private boolean available;      // 是否空闲可接单
    private Double rating;          // 司机评分
    private String vehicleType;     // ECONOMY, STANDARD, PREMIUM 等
    private LocalDateTime lastUpdateTime;
}
