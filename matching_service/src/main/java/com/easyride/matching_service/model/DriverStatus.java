package com.easyride.matching_service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.time.LocalDateTime;

@TableName("driver_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatus {
    @TableId(type = IdType.INPUT)
    private Long driverId; // Corresponds to User ID from User Service

    private double currentLatitude;
    private double currentLongitude;
    private boolean available;
    private double rating; // Synced or calculated
    private String vehicleType; // e.g., SEDAN, SUV (synced from User Service)

    private LocalDateTime lastLocationUpdateTime;
    private LocalDateTime lastStatusUpdateTime;
    private LocalDateTime onlineSince; // Track when driver came online

    private String currentCity; // Can be derived from lat/lon or set by driver

    private int currentPassengersInCar; // For carpooling
    private int maxCapacityForCarpool; // Vehicle's capacity for carpooling

    @Version
    private Long version;
}
