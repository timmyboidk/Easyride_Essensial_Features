package com.easyride.user_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.user_service.model.Admin;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
