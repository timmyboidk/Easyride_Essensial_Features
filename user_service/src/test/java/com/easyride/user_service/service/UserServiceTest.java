package com.easyride.user_service.service;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.exception.OtpVerificationException;
import com.easyride.user_service.exception.UserAlreadyExistsException;
import com.easyride.user_service.model.*;
import com.easyride.user_service.repository.*;
import com.easyride.user_service.rocket.UserRocketProducer;
import com.easyride.user_service.security.JwtTokenProvider;
import com.easyride.user_service.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
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
}
