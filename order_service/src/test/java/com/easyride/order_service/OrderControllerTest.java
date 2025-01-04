package com.easyride.order_service;

import com.easyride.order_service.controller.OrderController;
import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrder_Success() throws Exception {
        // 准备请求体
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        // 其他字段赋值，如 startLocation, endLocation, vehicleType, serviceType, paymentMethod
        // ...

        // 模拟返回结果
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(100L);
        responseDto.setStatus(OrderStatus.PENDING);
        responseDto.setPassengerName("TestPassenger");
        responseDto.setDriverName("TestDriver");
        responseDto.setEstimatedCost(100.0);
        responseDto.setEstimatedDistance(200.0);
        responseDto.setEstimatedDuration(60.0);

        when(orderService.createOrder(ArgumentMatchers.any(OrderCreateDto.class)))
                .thenReturn(responseDto);

        // 发送 POST 请求
        mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.passengerName").value("TestPassenger"))
                .andExpect(jsonPath("$.driverName").value("TestDriver"));

        // 验证 service 调用
        verify(orderService).createOrder(ArgumentMatchers.any(OrderCreateDto.class));
    }

    @Test
    void createOrder_ValidationError() throws Exception {
        // 不设置必需字段，触发验证错误
        OrderCreateDto createDto = new OrderCreateDto();

        mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDto)))
                .andExpect(status().isBadRequest());
    }

    // 其他接口测试: acceptOrder, cancelOrder, updateOrderStatus 等，同理
}
