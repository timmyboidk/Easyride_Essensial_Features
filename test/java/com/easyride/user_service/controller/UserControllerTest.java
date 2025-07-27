package com.easyride.user_service.controller;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.security.JwtTokenProvider;
import com.easyride.user_service.service.OtpService;
import com.easyride.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
/**
 * UserController 的Web层单元测试.
 * @WebMvcTest(UserController.class) 只加载UserController和Web相关的组件.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    // MockMvc 是我们模拟HTTP请求的入口
    @Autowired
    private MockMvc mockMvc;

    // ObjectMapper 用于在Java对象和JSON字符串之间进行转换
    @Autowired
    private ObjectMapper objectMapper;

    // @MockBean 创建一个UserService的模拟Bean.
    // 它会替换掉UserController中真正的UserService依赖.
    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;
    // ^^^^  这就是唯一的、需要添加的一行代码！ ^^^^
    @MockBean
    private OtpService otpService;
    // ^^^^  这就是我们通往胜利的最后一行代码！ ^^^^

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("POST /users/register - 当提供有效数据时, 应该注册成功并返回200 OK")
    void whenRegisterWithValidData_shouldReturnSuccess() throws Exception {
        // --- Given (准备阶段) ---
        // 1. 准备输入DTO，这部分逻辑完全来自您的代码，是正确的
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newUser");
        registrationDto.setPassword("password123");
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setPhoneNumber("1231231234");
        registrationDto.setRole("PASSENGER");
        registrationDto.setOtpCode("123456");

        // 2. 准备Service层应该返回的响应，这部分也完全正确
        UserRegistrationResponseDto serviceResponse = new UserRegistrationResponseDto(
                1L, "newUser", "newuser@example.com", "1231231234", "PASSENGER", true
        );

        // 3. 设定模拟Service的行为
        when(userService.registerUser(any(UserRegistrationDto.class), any(), any())).thenReturn(serviceResponse);

        // 4. 因为接口需要multipart/form-data, 我们需要把DTO对象包装成一个模拟的JSON文件部分
        String dtoString = objectMapper.writeValueAsString(registrationDto);
        MockMultipartFile registrationDtoPart = new MockMultipartFile(
                "registrationDto", // 这个名字必须和@RequestPart("registrationDto")中的值完全一样
                "",
                "application/json", // 明确告诉服务器这是一个JSON字符串
                dtoString.getBytes()
        );

        // --- When & Then (执行并验证) ---
        mockMvc.perform(
                        // 1. 使用 multipart() 来发送 multipart/form-data 请求
                        multipart("/users/register") // 2. 使用正确的URL: /users/register
                                .file(registrationDtoPart) // 3. 将我们包装好的JSON部分作为文件上传
                )
                // 4. 断言HTTP状态码为200 (OK)
                .andExpect(status().isOk())
                // 5. 断言响应体中的 message 字段
                .andExpect(jsonPath("$.message").value("注册成功"))
                // 6. 断言响应体中 data.username 字段的值是我们期望的
                .andExpect(jsonPath("$.data.username").value("newUser"));
    }

    @Test
    @DisplayName("POST /api/auth/login/otp - 当提供有效的手机和OTP时, 应该登录成功并返回Token")
    void whenLoginWithValidOtp_shouldReturnJwt() throws Exception {
        // --- Given ---
        // 1. 准备登录请求DTO
        PhoneOtpLoginRequestDto loginDto = new PhoneOtpLoginRequestDto();
        // 2. 使用 setter 方法为它赋值
        loginDto.setPhoneNumber("1234567890");
        loginDto.setOtpCode("123456"); // 基于您 service 代码中的 .getOtpCode() 推断出 setter 方法名

        // 2. 准备Service层应该返回的JWT响应
        JwtAuthenticationResponse serviceResponse = new JwtAuthenticationResponse("a-valid-jwt-token");

        // 3. 设定模拟Service的行为
        when(userService.loginWithPhoneOtp(any(PhoneOtpLoginRequestDto.class))).thenReturn(serviceResponse);

        // --- When & Then ---
        mockMvc.perform(
                        // 1. 使用 post() 来发送 application/json 请求
                        post("/users/login/otp") // 2. 使用正确的URL: /users/login/otp
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDto))
                                // 3. 以“匿名访客”身份访问
                                .with(anonymous())
                                // 4. 带上CSRF令牌
                                .with(csrf())
                )
                // 5. 断言HTTP状态码为200 (OK)
                .andExpect(status().isOk())
                // 6. 断言响应体中的 message 字段
                .andExpect(jsonPath("$.message").value("登录成功"))
                // 7. 断言响应体中 data.accessToken 字段的值是我们期望的
                .andExpect(jsonPath("$.data.accessToken").value("a-valid-jwt-token"));
    }

    @Test
    @DisplayName("POST /api/auth/login/otp - 当用户名为空时, 应该返回400 Bad Request")
    void whenRegisterWithBlankUsername_shouldReturnBadRequest() throws Exception {
        // --- Given ---
        // 准备一个不合法的DTO (username为空)
        UserRegistrationDto invalidDto = new UserRegistrationDto();
        invalidDto.setUsername(""); // 用户名为空，违反了 @NotBlank 约束
        invalidDto.setPassword("password123");
        invalidDto.setEmail("bad@req.com");
        // ... 其他字段 ...

        // --- When & Then ---
        // 在Controller层，因为有@Valid注解，请求在进入方法体之前就会被校验
        // 所以我们不需要模拟Service的行为，因为它根本不会被调用
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest()); // 断言HTTP状态码为400 (Bad Request)
    }
}
