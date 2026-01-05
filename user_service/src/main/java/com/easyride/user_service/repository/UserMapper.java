package com.easyride.user_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.user_service.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
