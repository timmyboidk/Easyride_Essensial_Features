package com.easyride.payment_service.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.easyride.payment_service.model.Payment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
