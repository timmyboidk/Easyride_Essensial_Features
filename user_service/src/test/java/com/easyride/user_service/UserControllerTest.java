package com.easyride.user_service.controller;

import com.easyride.user_service.api.UserApi;
import com.easyride.user_service.dto.*;
import com.easyride.user_service.security.JwtTokenProvider;
import com.easyride.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private ObjectMapper mapper = new ObjectMapper();

    // 辅助方法，计算签名
    private String computeSignature(String nonce, String timestamp, String secretKey) throws Exception {
        String data = nonce + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Test
    void testRegisterUser() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password");
        registrationDto.setEmail("test@example.com");
        registrationDto.setRole("PASSENGER");

        // 模拟 userService.registerUser 调用无返回值
        when(userService.registerUser(any(UserRegistrationDto.class))).then(invocation -> null);

        // 模拟签名头信息（签名校验在拦截器中执行，此处提供正确签名以便请求通过）
        String nonce = "nonce123";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String secretKey = "YourSecretKey";
        String signature = computeSignature(nonce, timestamp, secretKey);

        mockMvc.perform(post("/users/register")
                        .header("nonce", nonce)
                        .header("timestamp", timestamp)
                        .header("signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("注册成功"));
    }

    @Test
    void testAuthenticateUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password");

        JwtAuthenticationResponse jwtResponse = new JwtAuthenticationResponse("dummy-jwt-token");
        when(jwtTokenProvider.generateToken(any())).thenReturn("dummy-jwt-token");
        // 模拟认证过程省略细节

        String nonce = "nonce456";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String secretKey = "YourSecretKey";
        String signature = computeSignature(nonce, timestamp, secretKey);

        mockMvc.perform(post("/users/login")
                        .header("nonce", nonce)
                        .header("timestamp", timestamp)
                        .header("signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("dummy-jwt-token"));
    }
}
