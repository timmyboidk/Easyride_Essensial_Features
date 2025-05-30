package com.easyride.order_service.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
