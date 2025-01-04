package com.easyride.user_service.api;

import com.easyride.user_service.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

public interface UserApi {

    @PostMapping("/register")
    String registerUser(@Valid @RequestBody UserRegistrationDto registrationDto);

    @PostMapping("/login")
    JwtAuthenticationResponse authenticateUser(@Valid @RequestBody LoginRequest loginRequest);

    // 其他 API 方法...
}