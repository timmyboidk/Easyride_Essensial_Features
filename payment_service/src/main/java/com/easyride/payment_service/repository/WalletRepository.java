package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;



public interface WalletRepository extends JpaRepository<Wallet, Long> {

}

