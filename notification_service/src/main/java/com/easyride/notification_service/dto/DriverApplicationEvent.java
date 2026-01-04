package com.easyride.notification_service.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class DriverApplicationEvent implements Serializable {
    private Long driverUserId;
    private String applicationMaterialsLink;
    private Long applicationId;
}
