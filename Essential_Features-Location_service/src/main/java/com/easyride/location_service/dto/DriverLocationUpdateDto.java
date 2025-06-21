package com.easyride.location_service.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.Instant; // Use Instant for timestamps

@Data
public class DriverLocationUpdateDto {
    @NotNull(message = "纬度不能为空")
    private Double latitude;

    @NotNull(message = "经度不能为空")
    private Double longitude;

    private Long orderId; // Optional: If location is tied to a specific active order

    private Instant timestamp; // Client can send, or server can generate
}