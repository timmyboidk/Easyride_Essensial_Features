package com.easyride.matching_service.repository;

import com.easyride.matching_service.model.GrabbableOrder;
import com.easyride.matching_service.model.GrabbableOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface GrabbableOrderRepository extends JpaRepository<GrabbableOrder, Long> {
    List<GrabbableOrder> findByStatusAndExpiryTimeAfter(GrabbableOrderStatus status, LocalDateTime currentTime);
    // Add methods to find orders by region, vehicle type etc.
}