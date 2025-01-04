package com.easyride.user_service;

import com.easyride.user_service.config.SecurityConfig;
import com.easyride.user_service.controller.UserController;
import com.easyride.user_service.dto.LoginRequest;
import com.easyride.user_service.dto.UserRegistrationDto;
import com.easyride.user_service.security.JwtTokenProvider;
import com.easyride.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void registerUser_Success() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password123");
        registrationDto.setEmail("test@example.com");
        registrationDto.setRole("PASSENGER");

        doNothing().when(userService).registerUser(ArgumentMatchers.any(UserRegistrationDto.class));

        mockMvc.perform(post("/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("注册成功"));
    }

    @Test
    void registerUser_UserAlreadyExists() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("existinguser");
        registrationDto.setPassword("password123");
        registrationDto.setEmail("existing@example.com");
        registrationDto.setRole("PASSENGER");

        doThrow(new RuntimeException("用户名已存在"))
                .when(userService).registerUser(ArgumentMatchers.any(UserRegistrationDto.class));

        mockMvc.perform(post("/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("用户名已存在"));
    }

    @Test
    void loginUser_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        // 模拟认证和 JWT 生成
        when(authenticationManager.authenticate(any()))
                .thenReturn(null); // 简化处理

        when(jwtTokenProvider.generateToken(any()))
                .thenReturn("test_jwt_token");

        mockMvc.perform(post("/users/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test_jwt_token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }
}
