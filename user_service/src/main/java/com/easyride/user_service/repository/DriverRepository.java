package com.easyride.user_service.repository;

import com.easyride.user_service.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
