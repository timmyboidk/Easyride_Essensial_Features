package com.easyride.order_service.repository;

import com.easyride.order_service.model.Driver;
import com.easyride.order_service.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findFirstByVehicleTypeAndAvailableTrue(VehicleType vehicleType);
}
