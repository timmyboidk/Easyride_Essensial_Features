package com.easyride.user_service.service;

import com.easyride.user_service.dto.UserEventDto;
import com.easyride.user_service.dto.UserRegistrationDto;
import com.easyride.user_service.model.*;
import com.easyride.user_service.rocket.UserRocketProducer;
import com.easyride.user_service.repository.AdminRepository;
import com.easyride.user_service.repository.DriverRepository;
import com.easyride.user_service.repository.PassengerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceImplTest {

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
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_UsernameExists() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("existingUser");
        dto.setEmail("new@example.com");
        dto.setPassword("pass");
        dto.setRole("PASSENGER");

        when(passengerRepository.existsByUsername("existingUser")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(dto);
        });
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    void testRegisterUser_EmailExists() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("newUser");
        dto.setEmail("existing@example.com");
        dto.setPassword("pass");
        dto.setRole("DRIVER");

        when(passengerRepository.existsByUsername("newUser")).thenReturn(false);
        when(driverRepository.existsByUsername("newUser")).thenReturn(false);
        when(adminRepository.existsByUsername("newUser")).thenReturn(false);
        when(passengerRepository.existsByEmail("existing@example.com")).thenReturn(false);
        when(driverRepository.existsByEmail("existing@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            userService.registerUser(dto);
        });
        assertEquals("邮箱已注册", ex.getMessage());
    }

    @Test
    void testRegisterUser_Success_Passenger() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("newUser");
        dto.setEmail("new@example.com");
        dto.setPassword("pass");
        dto.setRole("PASSENGER");

        when(passengerRepository.existsByUsername("newUser")).thenReturn(false);
        when(driverRepository.existsByUsername("newUser")).thenReturn(false);
        when(adminRepository.existsByUsername("newUser")).thenReturn(false);
        when(passengerRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(driverRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(adminRepository.existsByEmail("new@example.com")).thenReturn(false);

        when(passwordEncoder.encode("pass")).thenReturn("encryptedPass");
        // 模拟保存 Passenger
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(invocation -> {
            Passenger p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // 不抛出异常，且调用消息发送
        assertDoesNotThrow(() -> userService.registerUser(dto));
        verify(userRocketProducer, times(1)).sendUserEvent(any(UserEventDto.class));
    }

    // 同理，可增加 DRIVER 和 ADMIN 的注册测试
}
