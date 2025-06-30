package com.easyride.order_service.security;

import com.easyride.order_service.model.User;
import com.easyride.order_service.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public OrderDetailServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OrderDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到：" + username));
        return OrderDetailsImpl.build(user);
    }

    @Transactional
    public OrderDetailsImpl loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到，ID：" + id));
        return OrderDetailsImpl.build(user);
    }
}
