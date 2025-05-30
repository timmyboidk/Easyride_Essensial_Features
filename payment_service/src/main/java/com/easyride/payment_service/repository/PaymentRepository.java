package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {


    Optional <Payment> findByOrderId(@Param("orderId") Long orderId);
}

