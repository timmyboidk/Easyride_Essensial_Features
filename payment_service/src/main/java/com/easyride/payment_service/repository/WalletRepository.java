package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface WalletRepository extends JpaRepository<Wallet, Long> {
    /**
     * 根据司机ID查找钱包.
     * @param driverId 司机的用户ID
     * @return 包含钱包的Optional，如果找不到则为空
     */
    Optional<Wallet> findByDriverId(Long driverId);
}

