package com.easyride.matching_service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

@TableName("matching_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId; // 哪个订单
    private Long driverId; // 分配给哪个司机
    private LocalDateTime matchedTime;
    private String matchStrategy; // 自动匹配或手动抢单
    private String status; // e.g., "ASSIGNED"
    private boolean success; // 是否成功（司机接受）或未成功
}
