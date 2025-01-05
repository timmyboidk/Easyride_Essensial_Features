package com.easyride.matching_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverStatusDto {

    private Long driverId;
    private Double latitude;
    private Double longitude;
    private boolean available;
    private Double rating;
    private String vehicleType;
}
