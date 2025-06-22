package com.easyride.matching_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matching_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;           // 哪个订单
    private Long driverId;          // 分配给哪个司机
    private LocalDateTime matchedTime;
    private String matchStrategy;   // 自动匹配或手动抢单
    private String status;          // e.g., "ASSIGNED"
    private boolean success;        // 是否成功（司机接受）或未成功
}
