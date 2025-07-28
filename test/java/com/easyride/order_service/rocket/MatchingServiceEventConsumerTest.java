package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.DriverAssignedEventDto;
import com.easyride.order_service.dto.OrderMatchFailedEventDto;
import com.easyride.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchingServiceEventConsumerTest {

    @Mock
    private OrderService orderService;

    // 我们需要一个真实的 ObjectMapper 实例来进行 JSON 序列化/反序列化
    // @Spy 会创建一个真实的对象，但我们仍然可以验证它的方法调用（如果需要）
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MatchingServiceEventConsumer matchingServiceEventConsumer;

    @Test
    void onMessage_whenReceivesDriverAssignedEvent_shouldCallProcessDriverAssigned() throws Exception {
        // 1. 准备 (Arrange)
        // 创建一个事件 DTO 对象
        DriverAssignedEventDto eventDto = new DriverAssignedEventDto(
                1L, 202L, "Test Driver", "PLATE-123", "Tesla Model Y", 5.0, null
        );
        // 将 DTO 对象转换为 JSON 字符串，模拟真实的消息内容
        String message = objectMapper.writeValueAsString(eventDto);

        // 2. 执行 (Act)
        matchingServiceEventConsumer.onMessage(message);

        // 3. 断言 (Assert)
        // 验证 orderService.processDriverAssigned 方法被调用
        ArgumentCaptor<DriverAssignedEventDto> captor = ArgumentCaptor.forClass(DriverAssignedEventDto.class);
        verify(orderService).processDriverAssigned(captor.capture());

        // 验证传递给 service 方法的 DTO 内容是否与原始消息一致
        DriverAssignedEventDto capturedDto = captor.getValue();
        assertEquals(1L, capturedDto.getOrderId());
        assertEquals(202L, capturedDto.getDriverId());
        assertEquals("PLATE-123", capturedDto.getVehiclePlate());
    }

    @Test
    void onMessage_whenReceivesMatchFailedEvent_shouldCallProcessOrderMatchFailed() throws Exception {
        // 1. 准备 (Arrange)
        OrderMatchFailedEventDto eventDto = new OrderMatchFailedEventDto(1L, "No drivers available");
        String message = objectMapper.writeValueAsString(eventDto);

        // 2. 执行 (Act)
        matchingServiceEventConsumer.onMessage(message);

        // 3. 断言 (Assert)
        // 验证 orderService.processOrderMatchFailed 方法被正确调用
        verify(orderService).processOrderMatchFailed(1L, "No drivers available");
    }
}