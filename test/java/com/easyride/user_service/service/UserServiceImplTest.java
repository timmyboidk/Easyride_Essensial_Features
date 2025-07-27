package com.easyride.user_service.service;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.exception.UserAlreadyExistsException;
import com.easyride.user_service.model.*;
import com.easyride.user_service.repository.*;
import com.easyride.user_service.rocket.UserRocketProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication; // 解决无法解析 Authentication 的问题
import org.springframework.security.crypto.password.PasswordEncoder;
import com.easyride.user_service.security.JwtTokenProvider; // 确保这是你项目中 JwtTokenProvider 的正确路径

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    // --- Mock 模拟对象 ---
    @Mock
    private UserRepository userRepository;

    @Mock
    private PassengerRepository passengerRepository; // <--- 新增的Mock

    @Mock
    private DriverRepository driverRepository; // <--- 新增的Mock

    @Mock
    private AdminRepository adminRepository; // <--- 新增的Mock

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRocketProducer userRocketProducer;

    @Mock
    private OtpService otpService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    // --- 被测试的类 ---
    @InjectMocks
    private UserServiceImpl userService;


    @Test
    @DisplayName("当新用户注册时, 应该能成功注册")
    void whenRegisterNewUser_thenRegistrationShouldBeSuccessful() {
        // --- Given (准备阶段) ---
        UserRegistrationDto registrationDto = new UserRegistrationDto(
                "testuser", "password123", "test@example.com",
                "PASSENGER", "1234567890", "123456", null, null
        );

        // 设定通用检查的剧本
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(otpService.validateOtp(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");

        // --- 这是最关键的修改 ---
        // 真实代码调用的是 passengerRepository.save()，所以我们必须模拟这个具体的调用。
        when(passengerRepository.save(any(Passenger.class))).thenAnswer(invocation -> { // <--- 修改
            Passenger userToSave = invocation.getArgument(0);
            userToSave.setId(1L); // 模拟数据库生成了ID
            return userToSave;
        });

        doNothing().when(userRocketProducer).sendUserEvent(any());

        // --- When (执行阶段) ---
        UserRegistrationResponseDto responseDto = userService.registerUser(registrationDto, null, null);

        // --- Then (验证阶段) ---
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getUsername()).isEqualTo("testuser");
        assertThat(responseDto.getId()).isEqualTo(1L);

        // 验证正确的 repository 的 save 方法被调用了
        verify(passengerRepository, times(1)).save(any(Passenger.class)); // <--- 修改
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRocketProducer, times(1)).sendUserEvent(any());
        // 同时也可以验证其他的 repository 没有被调用，让测试更严谨
        verify(driverRepository, never()).save(any());
    }

    @Test
    @DisplayName("当手机号已存在时, 应该抛出 UserAlreadyExistsException 异常")
    void whenPhoneNumberExists_thenShouldThrowUserAlreadyExistsException() {
        // --- Given ---
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setPhoneNumber("111222333");
        registrationDto.setRole("PASSENGER"); // 最好也提供角色

        when(userRepository.existsByPhoneNumber("111222333")).thenReturn(true);

        // --- When & Then ---
        UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDto, null, null);
        });

        assertThat(exception.getMessage()).isEqualTo("手机号已被注册");

        // 验证在失败场景下，任何 repository 的 save 方法都不能被调用
        verify(passengerRepository, never()).save(any());
        verify(driverRepository, never()).save(any());
        verify(adminRepository, never()).save(any());
        verify(userRocketProducer, never()).sendUserEvent(any());
    }

    @Test
    @DisplayName("当注册角色为司机时, 应该能成功注册司机")
    void whenRegisterNewDriver_thenRegistrationShouldBeSuccessful() {
        // --- Given (准备阶段) ---
        // 注意 DTO 中的角色是 "DRIVER"
        UserRegistrationDto registrationDto = new UserRegistrationDto(
                "driver001", "strongpassword", "driver@example.com",
                "DRIVER", "555666777", "654321", null, null
        );
        // 司机的特有信息
        registrationDto.setDriverLicenseNumber("LICENSE-12345");

        // 设定所有检查都表示用户不存在
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(otpService.validateOtp(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        // 关键：设定 driverRepository.save() 的行为
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            // 获取到正要被保存的 Driver 对象
            Driver driverToSave = invocation.getArgument(0);

            // 在这里增加一个强大的验证！
            // 断言这个即将被保存的对象的角色确实是 DRIVER
            assertThat(driverToSave.getRole()).isEqualTo(Role.DRIVER);
            assertThat(driverToSave.getDriverLicenseNumber()).isEqualTo("LICENSE-12345");

            // 模拟数据库返回一个已保存的对象
            driverToSave.setId(2L);
            return driverToSave;
        });

        // --- When (执行阶段) ---
        UserRegistrationResponseDto responseDto = userService.registerUser(registrationDto, null, null);

        // --- Then (验证阶段) ---
        // 验证返回结果
        assertThat(responseDto).isNotNull();
        assertThat(responseDto.getId()).isEqualTo(2L);
        assertThat(responseDto.getUsername()).isEqualTo("driver001");

        // 验证正确的 Repository 被调用
        verify(driverRepository, times(1)).save(any(Driver.class));
        // 验证 PassengerRepository 没有被调用
        verify(passengerRepository, never()).save(any());
        // 验证消息仍然被发送
        verify(userRocketProducer, times(1)).sendUserEvent(any());
    }
//
//    @Test
//    @DisplayName("当使用正确的凭证登录时, 应该返回包含Token的响应")
//    void whenLoginWithCorrectCredentials_thenReturnsLoginResponse() {
//        // --- Given (准备阶段) ---
//        LoginRequest loginRequest = new LoginRequest("testuser", "password123");
//
//        // 模拟一个已经通过认证的 Authentication 对象
//        Authentication successfulAuthentication = mock(Authentication.class);
//
//        // 注意：getPrincipal() 通常返回一个 UserDetails 对象或你的自定义User对象
//        // 这里我们继续使用User对象来模拟
//        User authenticatedUser = new Passenger("testuser", "encoded_password", "test@example.com", "1234567890");
//        authenticatedUser.setId(1L);
//
//        // 当 AuthenticationManager 尝试认证时, 返回我们模拟的成功对象
//        when(authenticationManager.authenticate(
//                any(UsernamePasswordAuthenticationToken.class))
//        ).thenReturn(successfulAuthentication);
//
//        // 当从成功的认证对象中获取用户信息时，返回我们准备好的用户
//        when(successfulAuthentication.getPrincipal()).thenReturn(authenticatedUser);
//
//        // --- 关键修改 1 ---
//        // 精确地模拟：当用 successfulAuthentication 对象去生成Token时，返回一个假Token
//        when(jwtTokenProvider.generateToken(successfulAuthentication)).thenReturn("dummy-jwt-token");
//
//        // --- When (执行阶段) ---
//        // --- 关键修改 2 ---
//        // 使用你项目中真实的 JwtAuthenticationResponse 类
//        JwtAuthenticationResponse jwtResponse = userService.loginUser(loginRequest);
//
//        // --- Then (验证阶段) ---
//        // --- 关键修改 3 ---
//        // 验证 JwtAuthenticationResponse 中的字段
//        assertThat(jwtResponse).isNotNull();
//        // 你的 JwtAuthenticationResponse 类中包含Token的字段是 accessToken
//        assertThat(jwtResponse.getAccessToken()).isEqualTo("dummy-jwt-token");
//    }
    @Test
    @DisplayName("当使用正确的手机号和OTP登录时, 应该返回JWT响应")
    void whenLoginWithPhoneOtp_withValidCredentials_shouldReturnJwt() {
        // --- Given (准备阶段) ---

        // vvvv  这是唯一的修改之处 vvvv
        // 1. 使用无参构造函数创建一个空的对象
        PhoneOtpLoginRequestDto loginDto = new PhoneOtpLoginRequestDto();
        // 2. 使用 setter 方法为它赋值
        loginDto.setPhoneNumber("1234567890");
        loginDto.setOtpCode("123456"); // 基于您 service 代码中的 .getOtpCode() 推断出 setter 方法名
        // ^^^^  这是唯一的修改之处 ^^^^

        // --- 后续的模拟行为和断言完全保持不变 ---
        User mockUser = new Passenger("testuser", "encoded_password", "test@example.com", "1234567890");
        mockUser.setId(1L);

        when(otpService.validateOtp("1234567890", "123456")).thenReturn(true);
        when(userRepository.findByPhoneNumber("1234567890")).thenReturn(Optional.of(mockUser));
        when(jwtTokenProvider.generateToken(any(Authentication.class))).thenReturn("a-real-jwt-token");

        // --- When (执行阶段) ---
        JwtAuthenticationResponse response = userService.loginWithPhoneOtp(loginDto);

        // --- Then (验证阶段) ---
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("a-real-jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(jwtTokenProvider, times(1)).generateToken(any(Authentication.class));
    }

}