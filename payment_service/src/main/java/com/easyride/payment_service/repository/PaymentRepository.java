package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPassengerId(Long passengerId);

    List<Payment> findByOrderId(Long orderId);
}

