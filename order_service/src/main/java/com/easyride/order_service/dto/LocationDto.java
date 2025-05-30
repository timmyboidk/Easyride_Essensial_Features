package com.easyride.order_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    private double latitude;

    private double longitude;
}

