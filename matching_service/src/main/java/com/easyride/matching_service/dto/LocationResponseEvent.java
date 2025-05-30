package com.easyride.matching_service.dto;

import lombok.*;

/**
 * location_service 回应时发送此事件
 * matching_service 监听该事件后，根据 correlationId 找到原请求并处理返回地址信息
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocationResponseEvent {

    private String correlationId;
    private String formattedAddress;      // 谷歌地图 API 返回的地址
    private String placeId;              // 例如 Google Place ID
    // 也可加入更多字段，如行政区、城市、邮编等
}
