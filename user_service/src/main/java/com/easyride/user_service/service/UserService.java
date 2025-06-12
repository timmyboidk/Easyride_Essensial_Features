package com.easyride.user_service.service;
import com.easyride.user_service.dto.*;
import com.easyride.user_service.model.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    // Modified to include files and return a DTO
    UserRegistrationResponseDto registerUser(UserRegistrationDto registrationDto, MultipartFile driverLicenseFile, MultipartFile vehicleDocumentFile);

    void requestOtpForLogin(String phoneNumber); // New
    JwtAuthenticationResponse loginWithPhoneOtp(PhoneOtpLoginRequestDto loginDto); // New

    User findByUsername(String username);

    // Other methods from prompts will be added here
    String requestOtpForPasswordReset(String identifier); // Identifier can be email or phone
    void resetPasswordWithOtp(ResetPasswordRequestDto resetPasswordDto);

    UserProfileUpdateDto updateUserProfile(Long userId, UserProfileUpdateDto profileUpdateDto);

    User updateUserProfile(String username,UserProfileUpdateDto profileUpdateDto);
    // Or more specific ones:
    // PassengerProfileDto updatePassengerProfile(Long userId, PassengerProfileUpdateDto passengerProfileDto);
    // DriverProfileDto updateDriverProfile(Long userId, DriverProfileUpdateDto driverProfileDto, List<MultipartFile> documents);
}