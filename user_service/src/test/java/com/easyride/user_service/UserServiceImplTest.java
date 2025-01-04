package com.easyride.user_service;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.model.Passenger;
import com.easyride.user_service.model.Admin;
import com.easyride.user_service.repository.PassengerRepository;
import com.easyride.user_service.repository.DriverRepository;
import com.easyride.user_service.repository.AdminRepository;
import com.easyride.user_service.rocket.UserRocketProducer;
import com.easyride.user_service.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class UserServiceImplTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRocketProducer userRocketProducer;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // 初始化 Mockito 注解
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_Passenger_Success() {
        // 1. 准备测试数据
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("testPassenger");
        dto.setPassword("pass123");
        dto.setEmail("passenger@example.com");
        dto.setRole("PASSENGER");

        // 2. Mock 返回值：所有 repo 均不存在该用户名/邮箱
        when(passengerRepository.existsByUsername("testPassenger")).thenReturn(false);
        when(driverRepository.existsByUsername("testPassenger")).thenReturn(false);
        when(adminRepository.existsByUsername("testPassenger")).thenReturn(false);

        when(passengerRepository.existsByEmail("passenger@example.com")).thenReturn(false);
        when(driverRepository.existsByEmail("passenger@example.com")).thenReturn(false);
        when(adminRepository.existsByEmail("passenger@example.com")).thenReturn(false);

        // 3. Mock PasswordEncoder
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPassword123");

        // 4. 执行注册
        userService.registerUser(dto);

        // 5. 验证 passengerRepository.save() 被调用
        ArgumentCaptor<Passenger> passengerCaptor = ArgumentCaptor.forClass(Passenger.class);
        verify(passengerRepository).save(passengerCaptor.capture());
        Passenger savedPassenger = passengerCaptor.getValue();
        assertEquals("testPassenger", savedPassenger.getUsername());
        assertEquals("encodedPassword123", savedPassenger.getPassword());
        assertEquals("passenger@example.com", savedPassenger.getEmail());

        // 6. 验证 userRocketProducer.sendUserEvent() 被调用
        ArgumentCaptor<UserEventDto> eventCaptor = ArgumentCaptor.forClass(UserEventDto.class);
        verify(userRocketProducer).sendUserEvent(eventCaptor.capture());
        UserEventDto sentEvent = eventCaptor.getValue();
        assertEquals("testPassenger", sentEvent.getUsername());
        assertEquals("passenger@example.com", sentEvent.getEmail());
        assertEquals("PASSENGER", sentEvent.getRole());
    }

    @Test
    void registerUser_UsernameExists_ThrowsException() {
        // 1. 准备测试数据
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("existingUser");
        dto.setPassword("pass123");
        dto.setEmail("existing@example.com");
        dto.setRole("DRIVER");

        // 2. Mock 返回：用户名已经存在
        when(passengerRepository.existsByUsername("existingUser")).thenReturn(true);

        // 3. 执行并断言抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(dto);
        });
        assertEquals("用户名已存在", exception.getMessage());

        // 4. 验证后续 repo.save() 和 userRocketProducer.sendUserEvent() 不应被调用
        verify(passengerRepository, never()).save(any(Passenger.class));
        verify(userRocketProducer, never()).sendUserEvent(any(UserEventDto.class));
    }

    @Test
    void registerUser_EmailExists_ThrowsException() {
        // 1. 准备测试数据
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("newUser");
        dto.setPassword("pass123");
        dto.setEmail("existing@example.com");
        dto.setRole("ADMIN");

        // 2. Mock 返回：Email 已存在
        when(passengerRepository.existsByUsername("newUser")).thenReturn(false);
        when(driverRepository.existsByUsername("newUser")).thenReturn(false);
        when(adminRepository.existsByUsername("newUser")).thenReturn(false);

        when(passengerRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // 3. 执行并断言抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(dto);
        });
        assertEquals("邮箱已注册", exception.getMessage());

        // 4. 验证后续操作未被调用
        verify(adminRepository, never()).save(any(Admin.class));
        verify(userRocketProducer, never()).sendUserEvent(any(UserEventDto.class));
    }

    // 可根据需要，添加更多测试方法，如注册 DRIVER 成功、异常场景等
}
