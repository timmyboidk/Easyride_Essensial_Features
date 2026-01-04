package com.easyride.order_service.integration;

import com.easyride.order_service.controller.OrderController;
import com.easyride.order_service.dto.LocationDto;
import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.model.PaymentMethod;
import com.easyride.order_service.model.ServiceType;
import com.easyride.order_service.model.VehicleType;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable Security for edge case validation input testing
class SecurityEdgeCaseTest {

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
        when(signatureVerificationInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    void testValidOrder() throws Exception {
        OrderCreateDto dto = createValidOrder();
        when(orderService.createOrder(any())).thenReturn(new OrderResponseDto());

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        if (result.getResponse().getStatus() != 200) {
            org.junit.jupiter.api.Assertions.fail("Valid Order Failed: " + result.getResponse().getStatus() + ". Body: "
                    + result.getResponse().getContentAsString());
        }
    }

    @Test
    void testXssInjectionInNotes() throws Exception {
        // SCENARIO: User tries to inject a script in notes
        OrderCreateDto dto = createValidOrder();
        dto.setPassengerNotes("<script>alert('XSS')</script>");

        when(orderService.createOrder(any())).thenReturn(new OrderResponseDto());

        // We expect 200 OK because usually Spring doesn't auto-sanitize fields unless
        // XSS filter is explicitly configured.
        // This test serves as a verification that the system ACCEPTs input,
        // OR if we added @SafeHtml validation, it should return 400.
        // For this codebase, we assume no @SafeHtml yet, so we verify it doesn't crash.

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        if (result.getResponse().getStatus() != 200) {
            org.junit.jupiter.api.Assertions.fail("Expected 200 but got " + result.getResponse().getStatus()
                    + ". Body: " + result.getResponse().getContentAsString());
        }
    }

    @Test
    void testSqlInjectionAttempt() throws Exception {
        OrderCreateDto dto = createValidOrder();
        dto.setPassengerNotes("'; DROP TABLE orders; --");

        when(orderService.createOrder(any())).thenReturn(new OrderResponseDto());

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        if (result.getResponse().getStatus() != 200) {
            org.junit.jupiter.api.Assertions.fail("Expected 200 but got " + result.getResponse().getStatus()
                    + ". Body: " + result.getResponse().getContentAsString());
        }
    }

    @Test
    void testLargePayload() throws Exception {
        OrderCreateDto dto = createValidOrder();
        dto.setPassengerNotes("A".repeat(10000));

        when(orderService.createOrder(any())).thenReturn(new OrderResponseDto());

        org.springframework.test.web.servlet.MvcResult result = mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andReturn();

        if (result.getResponse().getStatus() != 200) {
            org.junit.jupiter.api.Assertions.fail("Expected 200 but got " + result.getResponse().getStatus()
                    + ". Body: " + result.getResponse().getContentAsString());
        }
    }

    @Test
    void testInvalidEnumValues() throws Exception {
        // SCENARIO: Sending invalid Enum string via JSON
        String jsonPayload = """
                {
                    "passengerId": 123,
                    "startLocation": {"latitude": 10, "longitude": 10},
                    "endLocation": {"latitude": 20, "longitude": 20},
                    "vehicleType": "INVALID_TYPE",
                    "serviceType": "NORMAL",
                    "paymentMethod": "CASH"
                }
                """;

        mockMvc.perform(post("/orders/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isBadRequest()); // Should fail deserialization
    }

    private OrderCreateDto createValidOrder() {
        OrderCreateDto dto = new OrderCreateDto();
        dto.setPassengerId(1L);
        dto.setStartLocation(new LocationDto(1.0, 1.0));
        dto.setEndLocation(new LocationDto(2.0, 2.0));
        dto.setVehicleType(VehicleType.ECONOMY);
        dto.setServiceType(ServiceType.NORMAL);
        dto.setPaymentMethod(PaymentMethod.CASH);
        return dto;
    }
}
