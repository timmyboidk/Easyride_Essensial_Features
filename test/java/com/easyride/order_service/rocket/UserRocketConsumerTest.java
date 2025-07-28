package com.easyride.order_service.rocket;

import com.easyride.order_service.dto.UserEventDto;
import com.easyride.order_service.model.Driver;
import com.easyride.order_service.model.Passenger;
import com.easyride.order_service.model.Role;
import com.easyride.order_service.repository.DriverRepository;
import com.easyride.order_service.repository.PassengerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRocketConsumerTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private UserRocketConsumer userRocketConsumer;

    // --- 测试场景1: 收到一个新的乘客信息 ---
    @Test
    void onMessage_whenNewPassengerEvent_shouldCreateAndSavePassenger() {
        // 1. 准备 (Arrange)
        // 模拟一个来自用户服务的“新用户注册”消息 (乘客)
        UserEventDto newUserEvent = new UserEventDto(101L, "newPassenger", "new@test.com", "PASSENGER");

        // 模拟当根据 ID 查找乘客时，返回空，表示这是一个新乘客
        when(passengerRepository.findById(101L)).thenReturn(Optional.empty());

        // 2. 执行 (Act)
        // 直接调用 onMessage 方法，模拟消息的接收和处理
        userRocketConsumer.onMessage(newUserEvent);

        // 3. 断言 (Assert)
        // 验证 passengerRepository.save 方法是否被调用了一次
        ArgumentCaptor<Passenger> passengerCaptor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository, times(1)).save(passengerCaptor.capture());

        // 捕获被保存的 Passenger 对象，并检查其属性是否正确
        Passenger savedPassenger = passengerCaptor.getValue();
        assertEquals(101L, savedPassenger.getId());
        assertEquals("newPassenger", savedPassenger.getUsername());
        assertEquals("new@test.com", savedPassenger.getEmail());
        assertEquals(Role.PASSENGER, savedPassenger.getRole());

        // 确保 driverRepository 在此场景下没有任何交互
        verify(driverRepository, never()).findById(anyLong());
        verify(driverRepository, never()).save(any(Driver.class));
    }

    // --- 测试场景2: 收到一个已存在的司机的信息更新 ---
    @Test
    void onMessage_whenExistingDriverEvent_shouldUpdateAndSaveDriver() {
        // 1. 准备 (Arrange)
        // 模拟一个来自用户服务的“用户信息更新”消息 (司机)
        UserEventDto updatedUserEvent = new UserEventDto(202L, "driverUpdated", "updated@test.com", "DRIVER");

        // 准备一个已存在于本地数据库的司机对象
        Driver existingDriver = new Driver();
        existingDriver.setId(202L);
        existingDriver.setUsername("driverOld");
        existingDriver.setEmail("old@test.com");

        // 模拟当根据 ID 查找司机时，返回这个已存在的司机
        when(driverRepository.findById(202L)).thenReturn(Optional.of(existingDriver));

        // 2. 执行 (Act)
        userRocketConsumer.onMessage(updatedUserEvent);

        // 3. 断言 (Assert)
        // 验证 driverRepository.save 方法被调用了一次
        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(driverRepository, times(1)).save(driverCaptor.capture());

        // 捕获被保存的 Driver 对象，检查其信息是否已被更新
        Driver savedDriver = driverCaptor.getValue();
        assertEquals(202L, savedDriver.getId());
        assertEquals("driverUpdated", savedDriver.getUsername()); // 验证用户名已更新
        assertEquals("updated@test.com", savedDriver.getEmail()); // 验证邮箱已更新
        assertEquals(Role.DRIVER, savedDriver.getRole());

        // 确保 passengerRepository 在此场景下没有任何交互
        verify(passengerRepository, never()).findById(anyLong());
        verify(passengerRepository, never()).save(any(Passenger.class));
    }
}