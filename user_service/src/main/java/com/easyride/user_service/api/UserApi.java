package com.easyride.user_service.api;

import com.easyride.user_service.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserApi {

    // 用户注册
    @PostMapping("/register") // from UserApi
    ApiResponse<UserRegistrationResponseDto> registerUser(@Valid @RequestPart("registrationDto") UserRegistrationDto registrationDto,
                                                          @RequestPart(value = "driverLicenseFile", required = false) MultipartFile driverLicenseFile,
                                                          @RequestPart(value = "vehicleDocumentFile", required = false) MultipartFile vehicleDocumentFile);

    @PostMapping("/login")
    ApiResponse<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest);

    // 其他 API 方法...
}