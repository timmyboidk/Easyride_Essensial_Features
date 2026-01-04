package com.easyride.matching_service.controller;

import com.easyride.matching_service.dto.*;
import com.easyride.matching_service.service.MatchingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchingController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchingService matchingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void matchDriver_Success() throws Exception {
        MatchRequestDto requestDto = new MatchRequestDto();
        requestDto.setOrderId(100L);
        // Add other required fields if any.

        DriverAssignedEventDto eventDto = new DriverAssignedEventDto();
        eventDto.setOrderId(100L);
        eventDto.setDriverId(200L);

        when(matchingService.findAndAssignDriver(any(MatchRequestDto.class))).thenReturn(eventDto);

        mockMvc.perform(post("/matching/matchDriver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.driverId").value(200));
    }

    @Test
    void matchDriver_Failure() throws Exception {
        MatchRequestDto requestDto = new MatchRequestDto();
        requestDto.setOrderId(100L);

        when(matchingService.findAndAssignDriver(any(MatchRequestDto.class))).thenReturn(null);

        mockMvc.perform(post("/matching/matchDriver")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk()) // It returns ApiResponse with error, so HTTP status might still be 200, but
                                            // body code is 404.
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("未能匹配到合适的司机"));
    }

    @Test
    void updateDriverStatus_Success() throws Exception {
        Long driverId = 200L;
        DriverStatusUpdateDto updateDto = new DriverStatusUpdateDto();
        updateDto.setAvailable(true);

        mockMvc.perform(post("/matching/driverStatus/" + driverId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("司机状态更新成功"));
    }

    @Test
    void acceptOrder_Success() throws Exception {
        Long orderId = 100L;
        when(matchingService.acceptOrder(eq(orderId), any(Long.class))).thenReturn(true);

        mockMvc.perform(post("/matching/orders/" + orderId + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("订单接受成功"));
    }

    @Test
    void acceptOrder_Failure() throws Exception {
        Long orderId = 100L;
        when(matchingService.acceptOrder(eq(orderId), any(Long.class))).thenReturn(false);

        mockMvc.perform(post("/matching/orders/" + orderId + "/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("订单无法接受（可能已被分配或状态无效）"));
    }

    @Test
    void getAvailableOrders_Success() throws Exception {
        AvailableOrderDto order = new AvailableOrderDto();
        order.setOrderId(100L);
        when(matchingService.getAvailableOrdersForDriver()).thenReturn(List.of(order));

        mockMvc.perform(get("/matching/orders/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderId").value(100));
    }
}
