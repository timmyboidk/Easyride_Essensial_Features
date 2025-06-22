package com.easyride.matching_service.repository;

import com.easyride.matching_service.model.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import  java.util.List;

public interface DriverStatusRepository extends JpaRepository<DriverStatus, Long> {

    // 可根据需要添加查询，如:
    List<DriverStatus> findByAvailableTrueAndVehicleType(String vehicleType);
    // or find drivers by vehicleType, rating, etc.
}
