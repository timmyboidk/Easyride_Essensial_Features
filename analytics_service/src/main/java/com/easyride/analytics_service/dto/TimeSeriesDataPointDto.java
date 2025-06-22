package com.easyride.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeSeriesDataPointDto {
    private String time;
    private double value;
}