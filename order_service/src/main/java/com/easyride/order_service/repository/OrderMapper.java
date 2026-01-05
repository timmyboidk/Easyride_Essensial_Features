package com.easyride.order_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.order_service.model.Order;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
