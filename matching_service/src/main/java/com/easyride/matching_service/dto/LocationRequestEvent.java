package com.easyride.matching_service.dto;

import lombok.*;
import java.util.UUID;

/**
 * 当 matching_service 需要获取某个坐标的地理信息时，发送此事件到 location_service
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocationRequestEvent {

    private String correlationId;    // 用于跟踪请求与响应
    private Double latitude;
    private Double longitude;
}
