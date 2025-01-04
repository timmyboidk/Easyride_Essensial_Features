package com.easyride.user_service.security;

import com.easyride.user_service.model.User;
import com.easyride.user_service.repository.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final AdminRepository adminRepository;

    public UserDetailsServiceImpl(PassengerRepository passengerRepository,
                                  DriverRepository driverRepository,
                                  AdminRepository adminRepository) {
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = null;

        user = passengerRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = driverRepository.findByUsername(username).orElse(null);
            if (user == null) {
                user = adminRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("用户未找到：" + username));
            }
        }

        return UserDetailsImpl.build(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = null;

        user = passengerRepository.findById(id).orElse(null);
        if (user == null) {
            user = driverRepository.findById(id).orElse(null);
            if (user == null) {
                user = adminRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("用户未找到，ID：" + id));
            }
        }

        return UserDetailsImpl.build(user);
    }
}

