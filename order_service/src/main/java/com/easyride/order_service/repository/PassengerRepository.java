package com.easyride.order_service.repository;

import com.easyride.order_service.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    // 可以添加自定义查询方法
}
