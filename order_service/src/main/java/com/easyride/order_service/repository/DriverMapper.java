package com.easyride.order_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.order_service.model.Driver;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DriverMapper extends BaseMapper<Driver> {
}
