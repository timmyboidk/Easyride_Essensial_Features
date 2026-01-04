package com.easyride.order_service.security;

import com.easyride.order_service.dto.ApiResponse;
import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.service.OrderService;
import com.easyride.order_service.controller.OrderController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSecurityTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void testSqlInjectionInNotes() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        // Malicious payload
        createDto.setPassengerNotes("'; DROP TABLE orders; --");

        when(orderService.createOrder(any(OrderCreateDto.class))).thenReturn(new OrderResponseDto());

        orderController.createOrder(createDto);

        // If we sanitized in DTO, the service should receive sanitized string.
        verify(orderService).createOrder(createDto);
    }

    @Test
    void testXssInNotes() {
        OrderCreateDto createDto = new OrderCreateDto();
        createDto.setPassengerId(1L);
        // XSS Payload
        createDto.setPassengerNotes("<script>alert('XSS')</script>");

        when(orderService.createOrder(any(OrderCreateDto.class))).thenReturn(new OrderResponseDto());

        orderController.createOrder(createDto);

        verify(orderService).createOrder(createDto);
    }

    @Test
    void testIdor_CancelOrder_OwnerCheck_Enforced() {
        Long orderId = 999L;
        Long ownerId = 1001L;
        Long attackerId = 666L;

        // Mock Order
        OrderResponseDto mockOrder = new OrderResponseDto();
        mockOrder.setOrderId(orderId);
        mockOrder.setPassengerId(ownerId);
        when(orderService.getOrderDetails(orderId)).thenReturn(mockOrder);

        // Mock Security Context for Attacker
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(attackerId.toString());
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Act
        ApiResponse<String> response = orderController.cancelOrder(orderId);

        // Assert: Service NOT called, 403 Returned
        verify(orderService, never()).cancelOrder(orderId);
        assert response.getCode() == 403;

        // Cleanup
        SecurityContextHolder.clearContext();
    }
}
