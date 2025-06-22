package com.easyride.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardSummaryDto {
    private long totalOrders;
    private double totalRevenue;
    private long newUsers;
    private long activeDrivers;
}