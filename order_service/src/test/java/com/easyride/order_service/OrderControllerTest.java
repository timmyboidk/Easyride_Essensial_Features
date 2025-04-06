package com.easyride.order_service.controller;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void testCreateOrder() throws Exception {
        OrderCreateDto createDto = new OrderCreateDto(
                1L,
                new LocationDto(10.0, 20.0),
                new LocationDto(30.0, 40.0),
                VehicleType.CAR,
                ServiceType.STANDARD,
                PaymentMethod.CASH
        );

        OrderResponseDto responseDto = new OrderResponseDto(
                100L,
                OrderStatus.PENDING,
                "PassengerName",
                "DriverName",
                120.0,
                50.0,
                30.0
        );

        when(orderService.createOrder(createDto)).thenReturn(responseDto);

        mockMvc.perform(post("/orders/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100L));
    }

    @Test
    void testAcceptOrder() throws Exception {
        // 接受订单接口无返回结果，仅校验状态码
        mockMvc.perform(post("/orders/200/accept")
                        .param("driverId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOrderDetails() throws Exception {
        OrderResponseDto responseDto = new OrderResponseDto(
                101L,
                OrderStatus.PENDING,
                "PassengerName",
                "DriverName",
                150.0,
                60.0,
                35.0
        );

        when(orderService.getOrderDetails(101L)).thenReturn(responseDto);

        mockMvc.perform(get("/orders/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(101L));
    }

    @Test
    void testCancelOrder() throws Exception {
        mockMvc.perform(post("/orders/102/cancel"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateOrderStatus() throws Exception {
        mockMvc.perform(post("/orders/103/status")
                        .param("status", OrderStatus.ACCEPTED.name()))
                .andExpect(status().isOk());
    }
}
