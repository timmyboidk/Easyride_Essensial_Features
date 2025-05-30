package com.easyride.user_service.service;

import com.easyride.user_service.dto.UserRegistrationDto;

public interface UserService {

    void registerUser(UserRegistrationDto registrationDto);
}
