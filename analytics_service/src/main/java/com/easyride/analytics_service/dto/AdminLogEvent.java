package com.easyride.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogEvent implements Serializable {
    private Long adminId;
    private String operationType;
    private String targetEntityId;
    private String operationDetails;
    private String ipAddress;
    private LocalDateTime timestamp;
}
