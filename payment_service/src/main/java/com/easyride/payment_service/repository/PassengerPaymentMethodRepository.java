package com.easyride.payment_service.repository;

import com.easyride.payment_service.model.PassengerPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PassengerPaymentMethodRepository extends JpaRepository<PassengerPaymentMethod, Long> {
    List<PassengerPaymentMethod> findByPassengerId(Long passengerId);
    Optional<PassengerPaymentMethod> findByPassengerIdAndIsDefaultTrue(Long passengerId);
    Optional<PassengerPaymentMethod> findByPaymentGatewayToken(String token);
    Optional<PassengerPaymentMethod> findByIdAndPassengerId(Long id, Long passengerId);
}