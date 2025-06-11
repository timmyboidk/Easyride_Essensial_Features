package com.easyride.user_service.service;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.rocket.UserRocketProducer;
import com.easyride.user_service.model.*;
import com.easyride.user_service.repository.*;
import com.easyride.user_service.security.UserDetailsImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.easyride.user_service.exception.UserAlreadyExistsException;
// import com.easyride.user_service.util.FileStorageService; // If implementing file storage here

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository; // Generic user repo
    private final PasswordEncoder passwordEncoder;
    private final UserRocketProducer userRocketProducer;
    private final OtpService otpService;
    // private final FileStorageService fileStorageService; // For handling document uploads

    @Autowired
    public UserServiceImpl(PassengerRepository passengerRepository,
                           DriverRepository driverRepository,
                           AdminRepository adminRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserRocketProducer userRocketProducer,
                           OtpService otpService
            /*FileStorageService fileStorageService*/) {
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRocketProducer = userRocketProducer;
        this.otpService = otpService;
        // this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public UserRegistrationResponseDto registerUser(UserRegistrationDto registrationDto,
                                                    MultipartFile driverLicenseFile,
                                                    MultipartFile vehicleDocumentFile) {
        log.info("Registering user with phone: {} and email: {}", registrationDto.getPhoneNumber(), registrationDto.getEmail());

        if (userRepository.existsByPhoneNumber(registrationDto.getPhoneNumber())) {
            log.warn("Phone number {} already exists.", registrationDto.getPhoneNumber());
            throw new UserAlreadyExistsException("手机号已被注册");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            log.warn("Email {} already exists.", registrationDto.getEmail());
            throw new UserAlreadyExistsException("邮箱已被注册");
        }
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            log.warn("Username {} already exists.", registrationDto.getUsername());
            throw new UserAlreadyExistsException("用户名已存在");
        }

        // OTP Verification (assuming OTP is sent separately and verified here during registration)
        // If OTP is part of registration DTO, it means it was sent and user entered it.
        if (registrationDto.getOtpCode() != null && !otpService.validateOtp(registrationDto.getPhoneNumber(), registrationDto.getOtpCode())) {
            log.warn("OTP verification failed for phone number {}.", registrationDto.getPhoneNumber());
            throw new OtpVerificationException("无效的OTP验证码");
        }

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        Role role = Role.valueOf(registrationDto.getRole().toUpperCase());
        User savedUserEntity;
        String eventType = "USER_CREATED";

        switch (role) {
            case PASSENGER:
                Passenger passenger = new Passenger(registrationDto.getUsername(), encodedPassword, registrationDto.getEmail(), registrationDto.getPhoneNumber());
                savedUserEntity = passengerRepository.save(passenger);
                break;
            case DRIVER:
                Driver driver = new Driver(registrationDto.getUsername(), encodedPassword, registrationDto.getEmail(), registrationDto.getPhoneNumber());
                driver.setDriverLicenseNumber(registrationDto.getDriverLicenseNumber());
                driver.setVehicleInfo(registrationDto.getVehicleInfo());
                // driver.setApprovalStatus(DriverApprovalStatus.PENDING); // Add this field to Driver model

                // Handle file uploads for DRIVER
                // String licensePath = null;
                // String vehicleDocPath = null;
                // if (driverLicenseFile != null && !driverLicenseFile.isEmpty()) {
                //     licensePath = fileStorageService.storeFile(driverLicenseFile, driver.getId(), "license");
                //     driver.setDriverLicenseDocumentPath(licensePath);
                // }
                // if (vehicleDocumentFile != null && !vehicleDocumentFile.isEmpty()) {
                //     vehicleDocPath = fileStorageService.storeFile(vehicleDocumentFile, driver.getId(), "vehicle_doc");
                //     driver.setVehicleDocumentPath(vehicleDocPath);
                // }
                savedUserEntity = driverRepository.save(driver);
                eventType = "DRIVER_APPLICATION_SUBMITTED"; // More specific event for drivers
                // Create and send DriverApplicationEventDto
                DriverApplicationEventDto driverAppEvent = new DriverApplicationEventDto(
                        savedUserEntity.getId(),
                        savedUserEntity.getUsername(),
                        registrationDto.getDriverLicenseNumber()
                        // Add paths to uploaded documents if applicable
                );
                userRocketProducer.sendDriverApplicationEvent(driverAppEvent);
                break;
            case ADMIN: // Should admin registration be public? Usually not.
                Admin admin = new Admin(registrationDto.getUsername(), encodedPassword, registrationDto.getEmail(), registrationDto.getPhoneNumber());
                savedUserEntity = adminRepository.save(admin);
                break;
            default:
                log.error("Invalid role type: {}", registrationDto.getRole());
                throw new RuntimeException("无效的角色类型");
        }

        log.info("User {} (ID: {}) of role {} persisted.", savedUserEntity.getUsername(), savedUserEntity.getId(), savedUserEntity.getRole());

        // Publish generic user event (or keep specific ones)
        UserEventDto userEvent = new UserEventDto(savedUserEntity.getId(), savedUserEntity.getUsername(), savedUserEntity.getEmail(), savedUserEntity.getRole().name(), eventType);
        userRocketProducer.sendUserEvent(userEvent);
        log.info("Published {} event for user ID: {}", eventType, savedUserEntity.getId());

        return new UserRegistrationResponseDto(
                savedUserEntity.getId(),
                savedUserEntity.getUsername(),
                savedUserEntity.getEmail(),
                savedUserEntity.getPhoneNumber(),
                savedUserEntity.getRole().name(),
                savedUserEntity.isEnabled()
        );
    }

    @Override
    public void requestOtpForLogin(String phoneNumber) {
        log.info("Requesting OTP for login for phone number: {}", phoneNumber);
        if (!userRepository.existsByPhoneNumber(phoneNumber)) {
            log.warn("Attempted OTP request for non-existent phone number: {}", phoneNumber);
            throw new ResourceNotFoundException("该手机号未注册");
        }
        String otp = otpService.generateOtp(phoneNumber);
        otpService.sendOtp(phoneNumber, otp);
        log.info("OTP sent for login to phone number: {}", phoneNumber);
    }

    @Override
    public JwtAuthenticationResponse loginWithPhoneOtp(PhoneOtpLoginRequestDto loginDto) {
        log.info("Attempting OTP login for phone number: {}", loginDto.getPhoneNumber());
        if (!otpService.validateOtp(loginDto.getPhoneNumber(), loginDto.getOtpCode())) {
            log.warn("OTP login failed for phone number {}: Invalid OTP", loginDto.getPhoneNumber());
            throw new OtpVerificationException("OTP验证失败或已过期");
        }

        User user = userRepository.findByPhoneNumber(loginDto.getPhoneNumber())
                .orElseThrow(() -> {
                    log.error("User not found by phone number {} after OTP validation.", loginDto.getPhoneNumber()); // Should not happen if OTP was key'd by phone
                    return new ResourceNotFoundException("用户未找到");
                });

        // Create Authentication object for JWT generation
        // Note: For OTP login, password is not used directly in this step.
        // UserDetails are still needed for Spring Security context.
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        log.info("OTP login successful for phone number: {}. JWT generated.", loginDto.getPhoneNumber());
        return new JwtAuthenticationResponse(jwt);
    }

    // Implement other UserService methods based on prompts...
}
    }
}

