package com.easyride.order_service.dto;

import com.easyride.order_service.model.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDto {

    @NotNull
    private Long passengerId;

    @NotNull
    private LocationDto startLocation;

    @NotNull
    private LocationDto endLocation;

    @NotNull
    private VehicleType vehicleType;

    @NotNull
    private ServiceType serviceType;

    @NotNull
    private PaymentMethod paymentMethod;
}
