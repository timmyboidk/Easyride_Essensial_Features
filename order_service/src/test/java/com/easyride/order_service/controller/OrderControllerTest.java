package com.easyride.order_service.controller;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private com.easyride.order_service.interceptor.SignatureVerificationInterceptor signatureVerificationInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void createOrder_Success() throws Exception {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        createDto.setStartLocation(new LocationDto(40.0, -74.0));
        createDto.setEndLocation(new LocationDto(40.1, -74.1));
        createDto.setServiceType(com.easyride.order_service.model.ServiceType.NORMAL);
        createDto.setVehicleType(com.easyride.order_service.model.VehicleType.ECONOMY);
        createDto.setPaymentMethod(com.easyride.order_service.model.PaymentMethod.ONLINE_PAYMENT);

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(100L);
        responseDto.setStatus(OrderStatus.PENDING_MATCH);

        when(orderService.createOrder(any(OrderCreateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.orderId").value(100));
    }

    @Test
    void acceptOrder_Success() throws Exception {
        Long orderId = 100L;
        Long driverId = 200L;

        mockMvc.perform(post("/orders/" + orderId + "/accept")
                .param("driverId", driverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("订单已接单"));
    }

    @Test
    void getOrderDetails_Success() throws Exception {
        Long orderId = 100L;
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(orderId);
        responseDto.setEstimatedCost(50.00);

        when(orderService.getOrderDetails(orderId)).thenReturn(responseDto);

        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.estimatedCost").value(50.00));
    }

    @Test
    void cancelOrder_Success() throws Exception {
        Long orderId = 100L;

        mockMvc.perform(post("/orders/" + orderId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("订单已取消"));
    }

    @Test
    void updateOrderStatus_Success() throws Exception {
        Long orderId = 100L;
        UpdateOrderStatusDto statusDto = new UpdateOrderStatusDto();
        statusDto.setStatus(OrderStatus.COMPLETED);

        mockMvc.perform(post("/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("订单状态已更新"));
    }
}
