package com.easyride.order_service.repository;

import com.easyride.order_service.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

@DataJpaTest
public class RepositoryTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private PassengerRepository passengerRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testOrderRepository_FindByStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        List<Order> orders = orderRepository.findByStatus(OrderStatus.PENDING.name());
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void testOrderRepository_FindByDriverIdAndStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);
        Driver driver = new Driver();
        driver.setAvailable(true);
        driverRepository.save(driver);
        order.setDriver(driver);
        orderRepository.save(order);

        List<Order> orders = orderRepository.findByDriverIdAndStatus(driver.getId(), OrderStatus.PENDING.name());
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getDriver().getId()).isEqualTo(driver.getId());
    }

    @Test
    void testOrderRepository_FindByPassengerId() {
        Order order = new Order();
        Passenger passenger = new Passenger();
        passenger.setName("Test Passenger");
        passengerRepository.save(passenger);
        order.setPassenger(passenger);
        orderRepository.save(order);

        List<Order> orders = orderRepository.findByPassengerId(passenger.getId());
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getPassenger().getId()).isEqualTo(passenger.getId());
    }

    @Test
    void testDriverRepository_FindFirstByVehicleTypeAndAvailableTrue() {
        Driver driver = new Driver();
        driver.setVehicleType(VehicleType.CAR);
        driver.setAvailable(true);
        driverRepository.save(driver);

        Optional<Driver> found = driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.CAR);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(driver.getId());
    }

    @Test
    void testPassengerRepository_CRUD() {
        Passenger passenger = new Passenger();
        passenger.setName("Test Passenger");
        Passenger saved = passengerRepository.save(passenger);
        Optional<Passenger> found = passengerRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Passenger");
    }

    @Test
    void testUserRepository_CRUD() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole("PASSENGER");
        User saved = userRepository.save(user);
        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }
}
