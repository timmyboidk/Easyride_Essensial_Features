package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

    List<Withdrawal> findByDriverId(Long driverId);
}

