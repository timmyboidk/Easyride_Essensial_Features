package com.easyride.order_service;

import com.easyride.order_service.dto.*;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.service.OrderServiceImpl;
import com.easyride.order_service.util.DistanceCalculator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class OrderServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final PassengerRepository passengerRepository = mock(PassengerRepository.class);
    private final DriverRepository driverRepository = mock(DriverRepository.class);

    private final OrderServiceImpl orderService = new OrderServiceImpl(orderRepository, passengerRepository, driverRepository);

    @Test
    void createOrder_Success() {
        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setPassengerId(1L);
        orderCreateDto.setStartLocation(new LocationDto(30.0, 120.0));
        orderCreateDto.setEndLocation(new LocationDto(31.0, 121.0));
        orderCreateDto.setVehicleType(VehicleType.STANDARD);
        orderCreateDto.setServiceType(ServiceType.NORMAL);
        orderCreateDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        Passenger passenger = new Passenger(1L, "Test Passenger");
        Driver driver = new Driver(2L, "Test Driver", VehicleType.STANDARD, true);

        when(passengerRepository.findById(1L)).thenReturn(java.util.Optional.of(passenger));
        when(driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.STANDARD))
                .thenReturn(java.util.Optional.of(driver));

        orderService.createOrder(orderCreateDto);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(passenger, savedOrder.getPassenger());
        assertEquals(driver, savedOrder.getDriver());

        verify(driverRepository).save(driver);
        assertFalse(driver.isAvailable());
    }

    @Test
    void createOrder_NoAvailableDriver() {
        OrderCreateDto orderCreateDto = new OrderCreateDto();
        orderCreateDto.setPassengerId(1L);
        orderCreateDto.setStartLocation(new LocationDto(30.0, 120.0));
        orderCreateDto.setEndLocation(new LocationDto(31.0, 121.0));
        orderCreateDto.setVehicleType(VehicleType.STANDARD);
        orderCreateDto.setServiceType(ServiceType.NORMAL);
        orderCreateDto.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        Passenger passenger = new Passenger(1L, "Test Passenger");

        when(passengerRepository.findById(1L)).thenReturn(java.util.Optional.of(passenger));
        when(driverRepository.findFirstByVehicleTypeAndAvailableTrue(VehicleType.STANDARD))
                .thenReturn(java.util.Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(orderCreateDto);
        });

        assertEquals("没有可用的司机", exception.getMessage());
    }
}
