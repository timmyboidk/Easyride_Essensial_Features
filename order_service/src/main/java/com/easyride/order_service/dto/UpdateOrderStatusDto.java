package com.easyride.order_service.dto;

import com.easyride.order_service.model.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrderStatusDto {
    @NotNull(message = "订单状态不能为空")
    private OrderStatus status;
    // Optionally, add fields for who is updating, or any related data like cancellation reason if status is CANCELED
    // private String reason;
}