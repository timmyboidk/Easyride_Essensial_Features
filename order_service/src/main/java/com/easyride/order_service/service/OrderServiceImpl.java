package com.easyride.order_service.service;

import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.rocket.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.easyride.order_service.dto.*;
import com.easyride.order_service.exception.OrderServiceException;
import com.easyride.order_service.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final PassengerMapper passengerMapper;
    private final DriverMapper driverMapper;
    private final PricingService pricingService;
    private final OrderEventProducer orderEventProducer;

    public OrderServiceImpl(OrderMapper orderMapper,
            PassengerMapper passengerMapper,
            DriverMapper driverMapper,
            OrderEventProducer orderEventProducer,
            PricingService pricingService) {
        this.orderMapper = orderMapper;
        this.passengerMapper = passengerMapper;
        this.driverMapper = driverMapper;
        this.pricingService = pricingService;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto orderCreateDto) {
        log.info("Processing order creation request for passenger ID: {}", orderCreateDto.getPassengerId());
        Passenger passenger = passengerMapper.selectById(orderCreateDto.getPassengerId());
        if (passenger == null) {
            log.warn("Passenger entity not found for ID {}, creating a placeholder.",
                    orderCreateDto.getPassengerId());
            passenger = new Passenger();
            passenger.setId(orderCreateDto.getPassengerId());
            passenger.setUsername("Passenger " + orderCreateDto.getPassengerId()); // Placeholder name
            passengerMapper.insert(passenger);
        }

        PricingContext pricingContext = PricingContext.builder()
                .startLocation(orderCreateDto.getStartLocation())
                .endLocation(orderCreateDto.getEndLocation())
                .vehicleType(orderCreateDto.getVehicleType())
                .serviceType(orderCreateDto.getServiceType())
                .scheduledTime(orderCreateDto.getScheduledTime())
                .build();
        EstimatedPriceInfo estimatedPriceInfo = pricingService.calculateEstimatedPrice(pricingContext);

        Order order = new Order();
        order.setPassengerId(passenger.getId());
        order.setStartLatitude(orderCreateDto.getStartLocation().getLatitude());
        order.setStartLongitude(orderCreateDto.getStartLocation().getLongitude());
        order.setEndLatitude(orderCreateDto.getEndLocation().getLatitude());
        order.setEndLongitude(orderCreateDto.getEndLocation().getLongitude());
        order.setOrderTime(LocalDateTime.now());
        order.setEstimatedCost(estimatedPriceInfo.getEstimatedCost());
        order.setEstimatedDistance(estimatedPriceInfo.getEstimatedDistance());
        order.setEstimatedDuration(estimatedPriceInfo.getEstimatedDuration());
        order.setVehicleType(orderCreateDto.getVehicleType());
        order.setServiceType(orderCreateDto.getServiceType());
        order.setPaymentMethod(orderCreateDto.getPaymentMethod());
        order.setPassengerNotes(orderCreateDto.getPassengerNotes());
        order.setPassengerCount(orderCreateDto.getPassengerCount());
        order.setCreatedBy(passenger.getId());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        if (orderCreateDto.getScheduledTime() != null) {
            if (orderCreateDto.getScheduledTime().isBefore(LocalDateTime.now().plusMinutes(15))) {
                log.warn("Scheduled time {} is too soon.", orderCreateDto.getScheduledTime());
                throw new OrderServiceException("预约时间必须至少在当前时间15分钟后");
            }
            order.setScheduledTime(orderCreateDto.getScheduledTime());
            order.setStatus(OrderStatus.SCHEDULED);
        } else {
            order.setStatus(OrderStatus.PENDING_MATCH);
        }

        orderMapper.insert(order);
        Order savedOrder = order;
        log.info("Order (ID: {}) saved with status: {}. Scheduled time: {}", savedOrder.getId(), savedOrder.getStatus(),
                savedOrder.getScheduledTime());

        if (order.getStatus() == OrderStatus.PENDING_MATCH) {
            OrderCreatedEvent event = mapToOrderCreatedEvent(savedOrder, estimatedPriceInfo, passenger);
            orderEventProducer.sendOrderCreatedEvent(event);
            log.info("ORDER_CREATED event published for immediate order ID: {}", savedOrder.getId());
        }

        return mapToOrderResponseDto(savedOrder);
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long driverId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单不存在");
        }
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null) {
            throw new ResourceNotFoundException("司机不存在");
        }
        if (order.getDriverId() != null && order.getDriverId().equals(driver.getId())) {
            // Driver is already assigned to this order, proceed to accept
        } else if (!driver.isAvailable()) {
            throw new OrderServiceException("司机不可用");
        }
        order.setDriverId(driver.getId());
        order.setStatus(OrderStatus.ACCEPTED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(driverId);
        orderMapper.updateById(order);
        driver.setAvailable(false);
        driverMapper.updateById(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单不存在");
        }
        return mapToOrderResponseDto(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        log.info("Processing cancellation for order ID: {}", orderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单 " + orderId + " 未找到");
        }

        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setCancellationReason("Cancelled by user/system");

        if (order.getDriverId() != null) {
            Driver driver = driverMapper.selectById(order.getDriverId());
            if (driver != null) {
                driver.setAvailable(true);
                driverMapper.updateById(driver);
                log.info("Driver {} made available after order {} cancellation.", driver.getId(), orderId);
            }
        }
        orderMapper.updateById(order);
        log.info("Order ID: {} successfully cancelled.", orderId);

        OrderEventDto cancelEvent = new OrderEventDto(
                order.getId(),
                order.getPassengerId(),
                order.getDriverId(),
                OrderStatus.CANCELED,
                "订单已取消",
                LocalDateTime.now(),
                order.getStartLatitude(),
                order.getStartLongitude());
        orderEventProducer.sendOrderStatusUpdateEvent(cancelEvent);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单不存在");
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    @Override
    @Transactional
    public void processDriverAssigned(DriverAssignedEventDto event) {
        log.info("Processing driver assignment for order ID: {}", event.getOrderId());
        Order order = orderMapper.selectById(event.getOrderId());
        if (order == null) {
            throw new ResourceNotFoundException("订单 " + event.getOrderId() + " 未找到以分配司机");
        }

        if (order.getStatus() != OrderStatus.PENDING_MATCH && order.getStatus() != OrderStatus.SCHEDULED) {
            log.warn("Order {} is not in a state to be assigned a driver. Current status: {}", event.getOrderId(),
                    order.getStatus());
            return;
        }

        Driver driver = driverMapper.selectById(event.getDriverId());
        if (driver == null) {
            log.warn("Driver entity not found locally for ID {}, creating a placeholder.", event.getDriverId());
            driver = new Driver();
            driver.setId(event.getDriverId());
            driver.setUsername(
                    event.getDriverName() != null ? event.getDriverName() : "Driver " + event.getDriverId());
            driver.setAvailable(false);
            driverMapper.insert(driver);
        }

        if (!driver.isAvailable()) {
            log.warn("Driver {} was assigned to order {} but is no longer available. Requesting rematch or notifying.",
                    driver.getId(), order.getId());
            order.setStatus(OrderStatus.PENDING_MATCH);
            orderMapper.updateById(order);
            return;
        }

        order.setDriverId(driver.getId());
        order.setStatus(OrderStatus.DRIVER_ASSIGNED);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        driver.setAvailable(false);
        driverMapper.updateById(driver);
        log.info("Driver {} successfully assigned to order {}.", event.getDriverId(), event.getOrderId());

        OrderEventDto orderStatusUpdateEvent = new OrderEventDto(
                order.getId(), order.getPassengerId(), order.getDriverId(),
                OrderStatus.DRIVER_ASSIGNED, "司机已接单，正在前来", LocalDateTime.now(),
                order.getStartLatitude(), order.getStartLongitude());
        orderEventProducer.sendOrderStatusUpdateEvent(orderStatusUpdateEvent);
    }

    @Override
    @Transactional
    public void processOrderMatchFailed(Long orderId, String reason) {
        log.warn("Order match failed for order ID: {}. Reason: {}", orderId, reason);
        Order order = orderMapper.selectById(orderId);
        if (order != null
                && (order.getStatus() == OrderStatus.PENDING_MATCH || order.getStatus() == OrderStatus.SCHEDULED)) {
            order.setStatus(OrderStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            orderMapper.updateById(order);
            log.info("Order ID: {} marked as FAILED due to matching failure.", orderId);
        }
    }

    @Override
    @Transactional
    public void processPaymentConfirmation(Long orderId, Double finalAmount, String paymentTransactionId) {
        log.info("Processing payment confirmation for order ID: {}", orderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ResourceNotFoundException("订单 " + orderId + " 未找到");
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.PAYMENT_SETTLED) {
            order.setActualDropOffTime(LocalDateTime.now());
            PricingContext finalPricingContext = PricingContext.builder()
                    .startLocation(new LocationDto(order.getStartLatitude(), order.getStartLongitude()))
                    .endLocation(new LocationDto(order.getEndLatitude(), order.getEndLongitude()))
                    .vehicleType(order.getVehicleType())
                    .serviceType(order.getServiceType())
                    .build();
            FinalPriceInfo finalPrice = pricingService.calculateFinalPrice(order, finalPricingContext);
            order.setFinalCost(finalPrice.getFinalCost());
            order.setActualDistance(finalPrice.getActualDistance());
            order.setActualDuration(finalPrice.getActualDuration());
            order.setStatus(OrderStatus.PAYMENT_SETTLED);
            orderMapper.updateById(order);

            log.info("Order ID {} status updated to PAYMENT_SETTLED. Final cost: {}", orderId, finalAmount);

            OrderPaymentSettledEvent settledEvent = new OrderPaymentSettledEvent(
                    order.getId(),
                    order.getFinalCost(),
                    order.getPaymentMethod().name(),
                    LocalDateTime.now(),
                    order.getPassengerId(),
                    order.getDriverId());
            orderEventProducer.sendOrderPaymentSettledEvent(settledEvent);
        } else {
            log.warn("Order {} is not in COMPLETED state to confirm payment. Current status: {}", orderId,
                    order.getStatus());
        }
    }

    private OrderCreatedEvent mapToOrderCreatedEvent(Order order, EstimatedPriceInfo priceInfo, Passenger passenger) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getPassengerId(),
                order.getStartLatitude(),
                order.getStartLongitude(),
                order.getEndLatitude(),
                order.getEndLongitude(),
                order.getVehicleType(),
                order.getServiceType(),
                order.getPaymentMethod(),
                order.getEstimatedCost(),
                order.getScheduledTime(),
                order.getOrderTime(),
                passenger.getUsername());
    }

    private OrderResponseDto mapToOrderResponseDto(Order order) {
        Passenger passenger = passengerMapper.selectById(order.getPassengerId());
        Driver driver = order.getDriverId() != null ? driverMapper.selectById(order.getDriverId()) : null;
        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getPassengerId(),
                passenger != null ? passenger.getUsername() : "Unknown",
                driver != null ? driver.getUsername() : null,
                order.getEstimatedCost(),
                order.getEstimatedDistance(),
                order.getEstimatedDuration());
    }
}