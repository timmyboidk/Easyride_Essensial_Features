package com.easyride.user_service.service;

import com.easyride.user_service.dto.*;
import com.easyride.user_service.rocket.UserRocketProducer;
import com.easyride.user_service.model.*;
import com.easyride.user_service.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRocketProducer userRocketProducer;


    public UserServiceImpl(PassengerRepository passengerRepository,
                           DriverRepository driverRepository,
                           AdminRepository adminRepository,
                           PasswordEncoder passwordEncoder,
                           UserRocketProducer userRocketProducer) {
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRocketProducer = userRocketProducer;

    }

    @Override
    public void registerUser(UserRegistrationDto registrationDto) {
        // 检查用户名和邮箱是否已存在于任何一个仓库中
        if (passengerRepository.existsByUsername(registrationDto.getUsername()) ||
            driverRepository.existsByUsername(registrationDto.getUsername()) ||
            adminRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        if (passengerRepository.existsByEmail(registrationDto.getEmail()) ||
            driverRepository.existsByEmail(registrationDto.getEmail()) ||
            adminRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("邮箱已注册");
        }

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        Role role = Role.valueOf(registrationDto.getRole().toUpperCase());

        switch (role) {
            case PASSENGER:
                Passenger passenger = new Passenger(registrationDto.getUsername(), encodedPassword, registrationDto.getEmail());
                passengerRepository.save(passenger);

                // Publish user event
                UserEventDto userEventPassenger = new UserEventDto(passenger.getId(), passenger.getUsername(), passenger.getEmail(), "PASSENGER");
                userRocketProducer.sendUserEvent(userEventPassenger);
                break;

            case DRIVER:
                Driver driver = new Driver(registrationDto.getUsername(), encodedPassword, registrationDto.getEmail());
                driverRepository.save(driver);
                // Publish user event
                UserEventDto userEventDriver = new UserEventDto(driver.getId(), driver.getUsername(), driver.getEmail(), "DRIVER");
                userRocketProducer.sendUserEvent(userEventDriver);
                break;
            case ADMIN:
                Admin admin = new Admin(registrationDto.getUsername(), encodedPassword, registrationDto.getEmail());
                adminRepository.save(admin);
                // Publish user event
                UserEventDto userEventAdmin = new UserEventDto(admin.getId(), admin.getUsername(), admin.getEmail(), "ADMIN");
                userRocketProducer.sendUserEvent(userEventAdmin);
                break;
            default:
                throw new RuntimeException("无效的角色类型");
        }
    }
}

