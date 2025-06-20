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
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final PricingService pricingService;
    private final OrderEventProducer orderEventProducer;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            PassengerRepository passengerRepository,
                            DriverRepository driverRepository,
                            UserRepository userRepository,
                            OrderEventProducer orderEventProducer,
                            PricingService pricingService) {
        this.orderRepository = orderRepository;
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto orderCreateDto) {
        log.info("Processing order creation request for passenger ID: {}", orderCreateDto.getPassengerId());
        Passenger passenger = passengerRepository.findById(orderCreateDto.getPassengerId())
                .orElseGet(() -> {
                    log.warn("Passenger entity not found for ID {}, creating a placeholder.", orderCreateDto.getPassengerId());
                    Passenger newP = new Passenger();
                    newP.setId(orderCreateDto.getPassengerId());
                    newP.setName("Passenger " + orderCreateDto.getPassengerId()); // Placeholder name
                    return passengerRepository.save(newP);
                });

        PricingContext pricingContext = PricingContext.builder()
                .startLocation(orderCreateDto.getStartLocation())
                .endLocation(orderCreateDto.getEndLocation())
                .vehicleType(orderCreateDto.getVehicleType())
                .serviceType(orderCreateDto.getServiceType())
                .scheduledTime(orderCreateDto.getScheduledTime())
                .build();
        EstimatedPriceInfo estimatedPriceInfo = pricingService.calculateEstimatedPrice(pricingContext);

        Order order = new Order();
        order.setPassenger(passenger);
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
        order.setCreatedBy(passenger.getId());

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

        Order savedOrder = orderRepository.save(order);
        log.info("Order (ID: {}) saved with status: {}. Scheduled time: {}", savedOrder.getId(), savedOrder.getStatus(), savedOrder.getScheduledTime());

        if (order.getStatus() == OrderStatus.PENDING_MATCH) {
            OrderCreatedEvent event = mapToOrderCreatedEvent(savedOrder, estimatedPriceInfo);
            orderEventProducer.sendOrderCreatedEvent(event);
            log.info("ORDER_CREATED event published for immediate order ID: {}", savedOrder.getId());
        }

        return mapToOrderResponseDto(savedOrder);
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("司机不存在"));
        if (!driver.isAvailable()) {
            throw new OrderServiceException("司机不可用");
        }
        order.setDriver(driver);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(driverId);
        orderRepository.save(order);
        driver.setAvailable(false);
        driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        return mapToOrderResponseDto(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        log.info("Processing cancellation for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单 " + orderId + " 未找到"));

        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now());
        order.setCancellationReason("Cancelled by user/system");

        if (order.getDriver() != null) {
            Driver driver = order.getDriver();
            driver.setAvailable(true);
            driverRepository.save(driver);
            log.info("Driver {} made available after order {} cancellation.", driver.getId(), orderId);
        }
        orderRepository.save(order);
        log.info("Order ID: {} successfully cancelled.", orderId);

        OrderEventDto cancelEvent = new OrderEventDto(
                order.getId(),
                order.getPassenger().getId(),
                order.getDriver() != null ? order.getDriver().getId() : null,
                OrderStatus.CANCELED,
                "订单已取消",
                LocalDateTime.now(),
                order.getStartLatitude(),
                order.getStartLongitude()
        );
        orderEventProducer.sendOrderStatusUpdateEvent(cancelEvent);
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在"));
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void processDriverAssigned(DriverAssignedEventDto event) {
        log.info("Processing driver assignment for order ID: {}", event.getOrderId());
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("订单 " + event.getOrderId() + " 未找到以分配司机"));

        if (order.getStatus() != OrderStatus.PENDING_MATCH && order.getStatus() != OrderStatus.SCHEDULED) {
            log.warn("Order {} is not in a state to be assigned a driver. Current status: {}", event.getOrderId(), order.getStatus());
            return;
        }

        Driver driver = driverRepository.findById(event.getDriverId()).orElseGet(() -> {
            log.warn("Driver entity not found locally for ID {}, creating a placeholder.", event.getDriverId());
            Driver newDriver = new Driver();
            newDriver.setId(event.getDriverId());
            newDriver.setName(event.getDriverName() != null ? event.getDriverName() : "Driver " + event.getDriverId());
            newDriver.setAvailable(false);
            return driverRepository.save(newDriver);
        });

        if (!driver.isAvailable()) {
            log.warn("Driver {} was assigned to order {} but is no longer available. Requesting rematch or notifying.", driver.getId(), order.getId());
            order.setStatus(OrderStatus.PENDING_MATCH);
            orderRepository.save(order);
            return;
        }

        order.setDriver(driver);
        order.setStatus(OrderStatus.DRIVER_ASSIGNED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        driver.setAvailable(false);
        driverRepository.save(driver);
        log.info("Driver {} successfully assigned to order {}.", event.getDriverId(), event.getOrderId());

        OrderEventDto orderStatusUpdateEvent = new OrderEventDto(
                        order.getId(), order.getPassenger().getId(), order.getDriver().getId(),
                        OrderStatus.DRIVER_ASSIGNED, "司机已接单，正在前来", LocalDateTime.now(),
                        order.getStartLatitude(), order.getStartLongitude()
                );
        orderEventProducer.sendOrderStatusUpdateEvent(orderStatusUpdateEvent);
    }

    @Override
    @Transactional
    public void processOrderMatchFailed(Long orderId, String reason) {
        log.warn("Order match failed for order ID: {}. Reason: {}", orderId, reason);
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && (order.getStatus() == OrderStatus.PENDING_MATCH || order.getStatus() == OrderStatus.SCHEDULED)) {
            order.setStatus(OrderStatus.FAILED);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order ID: {} marked as FAILED due to matching failure.", orderId);
        }
    }

    @Override
    @Transactional
    public void processPaymentConfirmation(Long orderId, Double finalAmount, String paymentTransactionId) {
        log.info("Processing payment confirmation for order ID: {}", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单 " + orderId + " 未找到"));

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
            orderRepository.save(order);

            log.info("Order ID {} status updated to PAYMENT_SETTLED. Final cost: {}", orderId, finalAmount);

            OrderPaymentSettledEvent settledEvent = new OrderPaymentSettledEvent(
                    order.getId(),
                    order.getFinalCost(),
                    order.getPaymentMethod().name(),
                    LocalDateTime.now(),
                    order.getPassenger().getId(),
                    order.getDriver() != null ? order.getDriver().getId() : null
            );
            orderEventProducer.sendOrderPaymentSettledEvent(settledEvent);
        } else {
            log.warn("Order {} is not in COMPLETED state to confirm payment. Current status: {}", orderId, order.getStatus());
        }
    }

    private OrderCreatedEvent mapToOrderCreatedEvent(Order order, EstimatedPriceInfo priceInfo) {
        return new OrderCreatedEvent(
                order.getId(),
                order.getPassenger().getId(),
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
                order.getPassenger().getName()
        );
    }

    private OrderResponseDto mapToOrderResponseDto(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getPassenger().getName(),
                order.getDriver() != null ? order.getDriver().getName() : null,
                order.getEstimatedCost(),
                order.getEstimatedDistance(),
                order.getEstimatedDuration()
        );
    }
}