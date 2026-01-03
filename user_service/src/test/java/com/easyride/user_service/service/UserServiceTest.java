package com.easyride.user_service.service;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.exception.OtpVerificationException;
import com.easyride.user_service.exception.UserAlreadyExistsException;
import com.easyride.user_service.model.*;
import com.easyride.user_service.repository.*;
import com.easyride.user_service.rocket.UserRocketProducer;
import com.easyride.user_service.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserRocketProducer userRocketProducer;
    @Mock
    private OtpService otpService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;

    @BeforeEach
    void setUp() {
        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setPassword("password");
        registrationDto.setEmail("test@test.com");
        registrationDto.setPhoneNumber("1234567890");
        registrationDto.setRole("PASSENGER");
    }

    @Test
    void registerUser_Passenger_Success() {
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        Passenger passenger = new Passenger();
        passenger.setId(1L);
        passenger.setUsername("testuser");
        passenger.setRole(Role.PASSENGER);
        passenger.setEnabled(true);

        when(passengerRepository.save(any(Passenger.class))).thenReturn(passenger);

        UserRegistrationResponseDto response = userService.registerUser(registrationDto, null, null);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        verify(userRocketProducer, times(1)).sendUserEvent(any(UserEventDto.class));
    }

    @Test
    void registerUser_Driver_Success() {
        registrationDto.setRole("DRIVER");
        registrationDto.setDriverLicenseNumber("DL123");
        registrationDto.setRealName("Test Driver");
        registrationDto.setIdCardNumber("ID123");
        registrationDto.setCarModel("Toyota");
        registrationDto.setCarLicensePlate("ABC-123");

        MockMultipartFile driverLicenseFile = new MockMultipartFile("driverLicenseFile", "license.jpg", "image/jpeg",
                "test data".getBytes());
        MockMultipartFile vehicleDocumentFile = new MockMultipartFile("vehicleDocumentFile", "insurance.pdf",
                "application/pdf", "test data".getBytes());

        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(fileStorageService.storeFile(any(MultipartFile.class))).thenReturn("/path/to/file");

        Driver driver = new Driver();
        driver.setId(2L);
        driver.setUsername("testdriver");
        driver.setRole(Role.DRIVER);
        driver.setDriverLicenseNumber("DL123");
        driver.setEnabled(true);

        when(driverRepository.save(any(Driver.class))).thenReturn(driver);

        UserRegistrationResponseDto response = userService.registerUser(registrationDto, driverLicenseFile,
                vehicleDocumentFile);

        assertNotNull(response);
        assertEquals(2L, response.getUserId());
        verify(userRocketProducer, times(1)).sendDriverApplicationEvent(any(DriverApplicationEventDto.class));
    }

    @Test
    void registerUser_UserAlreadyExists() {
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDto, null, null);
        });
    }

    @Test
    void loginWithPhoneOtp_Success() {
        PhoneOtpLoginRequestDto loginDto = new PhoneOtpLoginRequestDto("1234567890", "123456");

        when(otpService.validateOtp("1234567890", "123456")).thenReturn(true);

        Passenger user = new Passenger();
        user.setId(1L);
        user.setPhoneNumber("1234567890");
        user.setUsername("testuser");
        user.setRole(Role.PASSENGER);
        user.setPassword("password");
        user.setEnabled(true);

        when(userRepository.findByPhoneNumber("1234567890")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(any())).thenReturn("mock-jwt-token");

        JwtAuthenticationResponse response = userService.loginWithPhoneOtp(loginDto);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getAccessToken());
    }

    @Test
    void loginWithPhoneOtp_InvalidOtp() {
        PhoneOtpLoginRequestDto loginDto = new PhoneOtpLoginRequestDto("1234567890", "wrong-otp");

        when(otpService.validateOtp("1234567890", "wrong-otp")).thenReturn(false);

        assertThrows(OtpVerificationException.class, () -> {
            userService.loginWithPhoneOtp(loginDto);
        });
    }

    @Test
    void registerUser_Admin_Success() {
        registrationDto.setRole("ADMIN");
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        Admin admin = new Admin();
        admin.setId(3L);
        admin.setUsername("testadmin");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        when(adminRepository.save(any(Admin.class))).thenReturn(admin);

        UserRegistrationResponseDto response = userService.registerUser(registrationDto, null, null);

        assertNotNull(response);
        assertEquals(3L, response.getUserId());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDto, null, null);
        });
    }

    @Test
    void registerUser_UsernameAlreadyExists() {
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> {
            userService.registerUser(registrationDto, null, null);
        });
    }

    @Test
    void registerUser_Driver_MissingLicense() {
        registrationDto.setRole("DRIVER");
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registrationDto, null, null);
        });
    }

    @Test
    void registerUser_Driver_MissingVehicleDoc() {
        registrationDto.setRole("DRIVER");
        MockMultipartFile driverLicenseFile = new MockMultipartFile("driverLicenseFile", "license.jpg", "image/jpeg",
                "test data".getBytes());

        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(fileStorageService.storeFile(any())).thenReturn("path");

        assertThrows(RuntimeException.class, () -> {
            userService.registerUser(registrationDto, driverLicenseFile, null);
        });
    }

    @Test
    void requestOtpForLogin_Success() {
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(true);
        when(otpService.generateOtp("1234567890")).thenReturn("123456");

        userService.requestOtpForLogin("1234567890");

        verify(otpService, times(1)).sendOtp(eq("1234567890"), eq("123456"));
    }

    @Test
    void requestOtpForLogin_UserNotFound() {
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);

        assertThrows(com.easyride.user_service.exception.ResourceNotFoundException.class, () -> {
            userService.requestOtpForLogin("1234567890");
        });
    }

    @Test
    void findByUsername_Success() {
        Passenger user = new Passenger();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.findByUsername("testuser");
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findByUsername_NotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(com.easyride.user_service.exception.ResourceNotFoundException.class, () -> {
            userService.findByUsername("testuser");
        });
    }

    @Test
    void requestOtpForPasswordReset_Success() {
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(true);
        when(otpService.generateOtp("1234567890")).thenReturn("123456");

        String otp = userService.requestOtpForPasswordReset("1234567890");
        assertEquals("123456", otp);
    }

    @Test
    void requestOtpForPasswordReset_NotFound() {
        when(userRepository.existsByPhoneNumber("1234567890")).thenReturn(false);

        assertThrows(com.easyride.user_service.exception.ResourceNotFoundException.class, () -> {
            userService.requestOtpForPasswordReset("1234567890");
        });
    }

    @Test
    void resetPasswordWithOtp_Success() {
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto("1234567890", "123456", "newPassword");
        when(otpService.validateOtp("1234567890", "123456")).thenReturn(true);

        Passenger user = new Passenger();
        when(userRepository.findByPhoneNumber("1234567890")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        userService.resetPasswordWithOtp(requestDto);

        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void resetPasswordWithOtp_InvalidOtp() {
        ResetPasswordRequestDto requestDto = new ResetPasswordRequestDto("1234567890", "wrong", "newPassword");
        when(otpService.validateOtp("1234567890", "wrong")).thenReturn(false);

        assertThrows(OtpVerificationException.class, () -> {
            userService.resetPasswordWithOtp(requestDto);
        });
    }

    @Test
    void updateUserProfile_Success() {
        UserProfileUpdateDto updateDto = new UserProfileUpdateDto();
        updateDto.setEmail("new@test.com");

        Passenger user = new Passenger();
        user.setUsername("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        User result = userService.updateUserProfile("testuser", updateDto);

        assertEquals("new@test.com", result.getEmail());
    }

    @Test
    void updateDriverProfile_Success() {
        DriverProfileUpdateDto updateDto = new DriverProfileUpdateDto();
        updateDto.setVerificationStatus(DriverApprovalStatus.APPROVED);
        updateDto.setReviewNotes("Looks good");

        Driver driver = new Driver();
        driver.setId(2L);
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));
        when(driverRepository.save(any())).thenReturn(driver);

        Driver result = userService.updateDriverProfile(2L, updateDto);

        assertEquals(DriverApprovalStatus.APPROVED, result.getVerificationStatus());
        assertEquals("Looks good", result.getReviewNotes());
    }

    @Test
    void updateDriverProfile_NotFound() {
        DriverProfileUpdateDto updateDto = new DriverProfileUpdateDto();
        when(driverRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(com.easyride.user_service.exception.ResourceNotFoundException.class, () -> {
            userService.updateDriverProfile(2L, updateDto);
        });
    }

    @Test
    void registerUser_OtpFailure() {
        registrationDto.setOtpCode("123456");
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(otpService.validateOtp("1234567890", "123456")).thenReturn(false);

        assertThrows(OtpVerificationException.class, () -> {
            userService.registerUser(registrationDto, null, null);
        });
    }
}
