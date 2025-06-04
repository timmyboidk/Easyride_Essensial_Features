package com.easyride.user_service.service;

import com.easyride.user_service.dto.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface UserService {
    // Modified to include files and return a DTO
    UserRegistrationResponseDto registerUser(UserRegistrationDto registrationDto, MultipartFile driverLicenseFile, MultipartFile vehicleDocumentFile);

    void requestOtpForLogin(String phoneNumber); // New
    JwtAuthenticationResponse loginWithPhoneOtp(PhoneOtpLoginRequestDto loginDto); // New

    // Other methods from prompts will be added here
    void requestOtpForPasswordReset(String identifier); // Identifier can be email or phone
    void resetPasswordWithOtp(ResetPasswordRequestDto resetPasswordDto);

    UserProfileUpdateDto updateUserProfile(Long userId, UserProfileUpdateDto profileUpdateDto);
    // Or more specific ones:
    // PassengerProfileDto updatePassengerProfile(Long userId, PassengerProfileUpdateDto passengerProfileDto);
    // DriverProfileDto updateDriverProfile(Long userId, DriverProfileUpdateDto driverProfileDto, List<MultipartFile> documents);
}