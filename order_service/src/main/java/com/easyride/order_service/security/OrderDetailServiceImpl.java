package com.easyride.order_service.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easyride.order_service.model.User;
import com.easyride.order_service.repository.UserMapper;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderDetailServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public OrderDetailServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public OrderDetailsImpl loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户未找到：" + username);
        }
        return OrderDetailsImpl.build(user);
    }

    @Transactional
    public OrderDetailsImpl loadUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new UsernameNotFoundException("用户未找到，ID：" + id);
        }
        return OrderDetailsImpl.build(user);
    }
}
