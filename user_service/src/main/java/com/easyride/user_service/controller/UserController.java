package com.easyride.user_service.controller;

import com.easyride.user_service.api.UserApi;
import com.easyride.user_service.dto.*;
import com.easyride.user_service.dto.ApiResponse;
import com.easyride.user_service.security.JwtTokenProvider;
import com.easyride.user_service.service.OtpService;
import com.easyride.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // For document upload

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/users")
public class UserController implements UserApi {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // 使用构造函数注入
    public UserController(UserService userService,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 用户注册
    @Override
    @PostMapping("/register")
    public ApiResponse<UserRegistrationResponseDto> registerUser(
            @Valid @RequestPart("registrationDto") UserRegistrationDto registrationDto,
            @RequestPart(value = "driverLicenseFile", required = false) MultipartFile driverLicenseFile,
            @RequestPart(value = "vehicleDocumentFile", required = false) MultipartFile vehicleDocumentFile) {
        log.info("Attempting to register user: {}", registrationDto.getUsername());
        // Pass files to service layer if role is DRIVER
        UserRegistrationResponseDto responseDto = userService.registerUser(registrationDto, driverLicenseFile,
                vehicleDocumentFile);
        log.info("User {} registered successfully.", responseDto.getUsername());
        return ApiResponse.success("注册成功", responseDto);
    }

    // 用户登录
    @Override
    @PostMapping("/login") // from UserApi
    public ApiResponse<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()// <-- 这里使用了密码
                ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        log.info("User {} logged in successfully.", loginRequest.getUsername());
        return ApiResponse.success(new JwtAuthenticationResponse(jwt));
    }

    @Autowired // Add if not already present for OtpService
    private OtpService otpService;

    @PostMapping("/otp/request-login")
    public ApiResponse<String> requestLoginOtp(@Valid @RequestBody RequestOtpDto requestOtpDto) {
        log.info("Requesting login OTP for phone: {}", requestOtpDto.getPhoneNumber());
        userService.requestOtpForLogin(requestOtpDto.getPhoneNumber());
        return ApiResponse.successMessage("OTP已发送至您的手机");
    }

    @PostMapping("/login/otp")
    public ApiResponse<JwtAuthenticationResponse> loginWithOtp(@Valid @RequestBody PhoneOtpLoginRequestDto loginDto) {
        log.info("Attempting login with OTP for phone: {}", loginDto.getPhoneNumber());
        JwtAuthenticationResponse response = userService.loginWithPhoneOtp(loginDto);
        return ApiResponse.success("登录成功", response);
    }

    @PostMapping("/auth/wechat")
    public ApiResponse<JwtAuthenticationResponse> loginWithWeChat(@RequestBody WeChatLoginRequest request) {
        log.info("Attempting WeChat login");
        JwtAuthenticationResponse response = userService.loginWithWeChat(request);
        return ApiResponse.success("WeChat Login Successful", response);
    }
}
