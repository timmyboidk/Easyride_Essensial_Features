package com.easyride.user_service.security;

import com.easyride.user_service.model.User;
import com.easyride.user_service.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // 使用构造函数注入
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到：" + username));
        return UserDetailsImpl.build(user);
    }

    // 根据用户 ID 加载用户
    @Transactional
    public UserDetailsImpl loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("用户未找到，ID：" + id));
        return UserDetailsImpl.build(user);
    }
}
