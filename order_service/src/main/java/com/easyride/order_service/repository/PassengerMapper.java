package com.easyride.order_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.order_service.model.Passenger;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PassengerMapper extends BaseMapper<Passenger> {
}
