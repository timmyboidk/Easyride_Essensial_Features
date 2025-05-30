package com.easyride.order_service.dto;

import com.easyride.order_service.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDto {

    private Long passengerId;

    private LocationDto startLocation;

    private LocationDto endLocation;

    private VehicleType vehicleType;

    private ServiceType serviceType;

    private PaymentMethod paymentMethod;
}

