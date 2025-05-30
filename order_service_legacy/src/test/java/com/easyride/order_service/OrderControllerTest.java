package com.easyride.order_service;

import com.easyride.order_service.config.SecurityConfig;
import com.easyride.order_service.controller.OrderController;
import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.security.JwtTokenProvider;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "testuser", roles = {"PASSENGER"})
    void createOrder_Success() throws Exception {
        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setPassengerId(1L);
        orderCreateDto.setStartLocation(new LocationDto(30.0, 120.0));
        orderCreateDto.setEndLocation(new LocationDto(31.0, 121.0));
        orderCreateDto.setVehicleType(VehicleType.STANDARD);
        orderCreateDto.setServiceType(ServiceType.NORMAL);
        orderCreateDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(100L);
        responseDto.setStatus(OrderStatus.PENDING);
        responseDto.setPassengerName("Test Passenger");
        responseDto.setDriverName("Test Driver");
        responseDto.setEstimatedCost(100.0);
        responseDto.setEstimatedDistance(200.0);
        responseDto.setEstimatedDuration(60.0);

        when(orderService.createOrder(ArgumentMatchers.any(OrderCreateDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/orders/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(orderCreateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.passengerName").value("Test Passenger"));
    }
}
