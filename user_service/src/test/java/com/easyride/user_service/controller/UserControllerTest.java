package com.easyride.user_service.controller;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.security.JwtTokenProvider;
import com.easyride.user_service.service.OtpService;
import com.easyride.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private OtpService otpService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerUser_Success() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPhoneNumber("1234567890");
        registrationDto.setRole("PASSENGER");

        UserRegistrationResponseDto responseDto = new UserRegistrationResponseDto(
                1L, "testuser", "test@example.com", "1234567890", "PASSENGER", true);

        when(userService.registerUser(any(UserRegistrationDto.class), any(), any())).thenReturn(responseDto);

        MockMultipartFile registrationDtoMulti = new MockMultipartFile("registrationDto", "", "application/json",
                objectMapper.writeValueAsString(registrationDto).getBytes());

        mockMvc.perform(multipart("/users/register")
                .file(registrationDtoMulti))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void authenticateUser_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateToken(any())).thenReturn("mock-token");

        mockMvc.perform(post("/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("mock-token"));
    }

    @Test
    void loginWithOtp_Success() throws Exception {
        PhoneOtpLoginRequestDto loginDto = new PhoneOtpLoginRequestDto("1234567890", "123456");
        JwtAuthenticationResponse response = new JwtAuthenticationResponse("mock-otp-token");

        when(userService.loginWithPhoneOtp(any())).thenReturn(response);

        mockMvc.perform(post("/users/login/otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("mock-otp-token"));
    }
}
