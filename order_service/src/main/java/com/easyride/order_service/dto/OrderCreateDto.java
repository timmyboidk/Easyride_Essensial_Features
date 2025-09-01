package com.easyride.order_service.dto;

import com.easyride.order_service.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDto {

    @NotNull(message = "乘客ID不能为空") // This might come from authenticated principal instead
    private Long passengerId;

    @NotNull(message = "起始位置不能为空")
    private LocationDto startLocation;

    @NotNull(message = "结束位置不能为空")
    private LocationDto endLocation;

    @NotNull(message = "车辆类型不能为空")
    private VehicleType vehicleType;

    @NotNull(message = "服务类型不能为空")
    private ServiceType serviceType;

    @NotNull(message = "支付方式不能为空")
    private PaymentMethod paymentMethod;

    private LocalDateTime scheduledTime; // New: For scheduled rides (nullable for immediate rides)

    private String passengerNotes;

    private Integer passengerCount;
}

