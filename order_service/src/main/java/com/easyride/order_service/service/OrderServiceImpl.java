package com.easyride.order_service.service;

import com.easyride.order_service.dto.OrderCreateDto;
import com.easyride.order_service.dto.OrderResponseDto;
import com.easyride.order_service.model.*;
import com.easyride.order_service.repository.*;
import com.easyride.order_service.rocket.OrderEventProducer;
import com.easyride.order_service.util.DistanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository; // 新增
    private final PricingService pricingService;// 新增

    public OrderServiceImpl(OrderRepository orderRepository,
                            PassengerRepository passengerRepository,
                            DriverRepository driverRepository,
                            UserRepository userRepository,
                            OrderEventProducer orderEventProducer,
                            PricingService pricingService) { // 更新构造函数
        this.orderRepository = orderRepository;
        this.passengerRepository = passengerRepository;
        this.driverRepository = driverRepository;
        this.userRepository = userRepository; // 初始化
        this.orderEventProducer = orderEventProducer;
        this.pricingService = pricingService;
    }

    @Override
    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto orderCreateDto) {
        log.info("Processing order creation request for passenger ID: {}", orderCreateDto.getPassengerId());
        // User passengerUser = userRepository.findById(orderCreateDto.getPassengerId())
        //         .orElseThrow(() -> new ResourceNotFoundException("乘客用户不存在: " + orderCreateDto.getPassengerId()));

        // if (!"PASSENGER".equalsIgnoreCase(passengerUser.getRole())) {
        //     throw new OrderServiceException("用户ID " + orderCreateDto.getPassengerId() + " 不是一个有效的乘客角色");
        // }

        // For now, assuming passengerId is validated and Passenger entity exists or is created based on it
        // This part needs robust user data handling, possibly fetching from User Service or relying on synced data
        Passenger passenger = passengerRepository.findById(orderCreateDto.getPassengerId())
                .orElseGet(() -> {
                    log.warn("Passenger entity not found for ID {}, creating a placeholder. Ensure UserRocketConsumer syncs users.", orderCreateDto.getPassengerId());
                    Passenger newP = new Passenger();
                    newP.setId(orderCreateDto.getPassengerId());
                    newP.setName("Passenger " + orderCreateDto.getPassengerId()); // Placeholder name
                    return passengerRepository.save(newP);
                });


        // Pricing and ETA estimation
        PricingContext pricingContext = PricingContext.builder()
                .startLocation(orderCreateDto.getStartLocation())
                .endLocation(orderCreateDto.getEndLocation())
                .vehicleType(orderCreateDto.getVehicleType())
                .serviceType(orderCreateDto.getServiceType())
                .scheduledTime(orderCreateDto.getScheduledTime())
                .build();
         EstimatedPriceInfo estimatedPriceInfo = pricingService.calculateEstimatedPrice(pricingContext);
         order.setEstimatedCost(estimatedPriceInfo.getEstimatedCost());
         order.setEstimatedDistance(estimatedPriceInfo.getEstimatedDistance());
         order.setEstimatedDuration(estimatedPriceInfo.getEstimatedDuration());

        Order order = new Order();
        order.setPassenger(passenger);
        order.setStartLatitude(orderCreateDto.getStartLocation().getLatitude());
        order.setStartLongitude(orderCreateDto.getStartLocation().getLongitude());
        order.setEndLatitude(orderCreateDto.getEndLocation().getLatitude());
        order.setEndLongitude(orderCreateDto.getEndLocation().getLongitude());
        order.setOrderTime(LocalDateTime.now());
        if (orderCreateDto.getScheduledTime() != null) {
            if (orderCreateDto.getScheduledTime().isBefore(LocalDateTime.now().plusMinutes(15))) { // Example: Must be at least 15 mins in future
                log.warn("Scheduled time {} is too soon.", orderCreateDto.getScheduledTime());
                throw new OrderServiceException("预约时间必须至少在当前时间15分钟后");
            }
            order.setScheduledTime(orderCreateDto.getScheduledTime());
            order.setStatus(OrderStatus.SCHEDULED); // New status for scheduled orders
        } else {
            order.setStatus(OrderStatus.PENDING_MATCH); // For immediate orders, pending matching
        }

        if (orderCreateDto.getServiceType() == ServiceType.CARPOOL) {
            log.info("Carpool order requested for passenger ID: {}", orderCreateDto.getPassengerId());
            // Additional logic for carpool:
            // - Maybe check if passenger opted-in for carpool if it's a preference
            // - PricingService will handle different cost estimation
            // - MatchingService will handle the actual carpool matching logic
        }

        order.setEstimatedCost(estimatedPriceInfo.getEstimatedCost());
        order.setEstimatedDistance(estimatedPriceInfo.getEstimatedDistance());
        order.setEstimatedDuration(estimatedPriceInfo.getEstimatedDuration()); // in minutes

        order.setVehicleType(orderCreateDto.getVehicleType());
        order.setServiceType(orderCreateDto.getServiceType());
        order.setPaymentMethod(orderCreateDto.getPaymentMethod());
        order.setPassengerNotes(orderCreateDto.getPassengerNotes());
        order.setCreatedBy(passenger.getId());

        Order savedOrder = orderRepository.save(order);
        log.info("Order (ID: {}) saved with status: {}. Scheduled time: {}", savedOrder.getId(), savedOrder.getStatus(), savedOrder.getScheduledTime());

        // If it's an immediate order, publish for matching. Scheduled orders might be published later by a scheduler job.
        if (order.getStatus() == OrderStatus.PENDING_MATCH) {
            OrderCreatedEvent event = mapToOrderCreatedEvent(savedOrder, estimatedPriceInfo);
            orderEventProducer.sendOrderCreatedEvent(event);
            log.info("ORDER_CREATED event published for immediate order ID: {}", savedOrder.getId());
        } else if (order.getStatus() == OrderStatus.SCHEDULED) {
            // Logic for handling scheduled orders (e.g., a background job to publish them near scheduled time)
            log.info("Order ID: {} is scheduled for {}. It will be processed later.", savedOrder.getId(), savedOrder.getScheduledTime());
        }

        return mapToOrderResponseDto(savedOrder); // Driver will be null at this stage
    }

    private OrderCreatedEvent mapToOrderCreatedEvent(Order order, EstimatedPriceInfo priceInfo) {
        // Map order details to OrderCreatedEvent for Matching Service
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
                order.getScheduledTime(), // Include scheduled time
                order.getOrderTime(),
                order.getPassenger().getName() // Example: include passenger name if needed by matching for context
        );

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long driverId) {
        // 查找订单
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 查找司机
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("司机不存在"));

        // 检查司机是否可用
        if (!driver.isAvailable()) {
            throw new RuntimeException("司机不可用");
        }

        // 更新订单状态并关联司机
        order.setStatus(OrderStatus.ACCEPTED);
        order.setDriver(driver);
        order.setUpdatedAt(LocalDateTime.now());
        order.setUpdatedBy(driverId);
        orderRepository.save(order);

        // 更新司机状态为不可用
        driver.setAvailable(false);
        driverRepository.save(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setOrderId(order.getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setPassengerName(order.getPassenger().getName());
        responseDto.setDriverName(order.getDriver() != null ? order.getDriver().getName() : null);
        responseDto.setEstimatedCost(order.getEstimatedCost());
        responseDto.setEstimatedDistance(order.getEstimatedDistance());
        responseDto.setEstimatedDuration(order.getEstimatedDuration());

        return responseDto;
    }

        @Override
        @Transactional
        public void cancelOrder(Long orderId /*, Long cancellingUserId, Role cancellingByRole */) {
            log.info("Processing cancellation for order ID: {}", orderId);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("订单 " + orderId + " 未找到"));

            // Validate if cancellation is allowed based on current status and who is cancelling
            // e.g., passenger can cancel if PENDING_MATCH, DRIVER_ASSIGNED (with potential fee)
            // driver might cancel under certain conditions (also with potential implications)

            LocalDateTime cancellationTime = LocalDateTime.now();
            double cancellationFee = 0.0;

            // Only apply fee if cancelled by passenger after certain stages
            // if (cancellingByRole == Role.PASSENGER) { // Assuming Role is available
            cancellationFee = pricingService.calculateCancellationFee(order, cancellationTime);
            // }

            order.setStatus(OrderStatus.CANCELED);
            order.setUpdatedAt(cancellationTime);
            order.setCancellationReason("Cancelled by user/system"); // Be more specific
            // order.setCancelledByUserId(cancellingUserId);
            // order.setCancelledByRole(cancellingByRole);

            if (cancellationFee > 0) {
                // order.setFinalCost(cancellationFee); // Set the fee as the final cost
                log.info("Cancellation fee of ${} applied to order {}", cancellationFee, orderId);
                // Trigger payment process for the cancellation fee
                // This would involve publishing an event that PaymentService consumes
                // Or directly calling PaymentService if synchronous and appropriate.
                // For now, just log and set. Actual charging is complex.
                // OrderPaymentSettledEvent feeEvent = new OrderPaymentSettledEvent(order.getId(), cancellationFee, ...);
                // orderEventProducer.sendOrderPaymentSettledEvent(feeEvent);
            }

            // If a driver was assigned, make them available again
            if (order.getDriver() != null) {
                Driver driver = order.getDriver();
                driver.setAvailable(true);
                driverRepository.save(driver);
                log.info("Driver {} made available after order {} cancellation.", driver.getId(), orderId);
            }
            orderRepository.save(order);
            log.info("Order ID: {} successfully cancelled.", orderId);

            // Publish cancellation event
            OrderEventDto cancelEvent = new OrderEventDto(
                    order.getId(), order.getPassenger().getId(), order.getDriver() != null ? order.getDriver().getId() : null,
                    OrderStatus.CANCELED.name(), LocalDateTime.now(),
                    "订单已取消",
                    order.getStartLatitude(), order.getStartLongitude()
            );
            orderEventProducer.sendOrderStatusUpdateEvent(cancelEvent);
        }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

        @Autowired // Add if not already present
        private DriverRepository driverRepository; // Assuming you have a local Driver model/repo

        @Override
        @Transactional
        public void processDriverAssigned(DriverAssignedEventDto event) {
            log.info("Processing driver assignment for order ID: {}", event.getOrderId());
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> {
                        log.error("Order not found for driver assignment. Order ID: {}", event.getOrderId());
                        return new ResourceNotFoundException("订单 " + event.getOrderId() + " 未找到以分配司机");
                    });

            if (order.getStatus() != OrderStatus.PENDING_MATCH && order.getStatus() != OrderStatus.SCHEDULED) {
                // SCHEDULED orders might be directly assigned if matching happens right after they become active
                log.warn("Order {} is not in a state to be assigned a driver. Current status: {}", event.getOrderId(), order.getStatus());
                // Potentially revert match in MatchingService or handle as business logic dictates
                return;
            }

            // Fetch or create a local representation of the driver
            // This relies on driver data being available or synced.
            // UserRocketConsumer should handle syncing basic User data (which Driver extends or relates to)
            Driver driver = driverRepository.findById(event.getDriverId())
                    .orElseGet(() -> {
                        log.warn("Driver entity not found locally for ID {}, creating a placeholder. Ensure UserRocketConsumer syncs drivers.", event.getDriverId());
                        Driver newDriver = new Driver();
                        newDriver.setId(event.getDriverId());
                        newDriver.setName(event.getDriverName() != null ? event.getDriverName() : "Driver " + event.getDriverId());
                        // Populate other fields from event if available
                        // newDriver.setVehicleType(...); // This should ideally come from User service sync
                        newDriver.setAvailable(false); // Assume assigned means not available for other orders
                        return driverRepository.save(newDriver);
                    });

            if (!driver.isAvailable()) {
                // This could happen in a race condition if driver took another order via a different mechanism
                log.warn("Driver {} was assigned to order {} but is no longer available. Requesting rematch or notifying.", driver.getId(), order.getId());
                // TODO: Handle this scenario - e.g., send event back to Matching Service to find another driver
                order.setStatus(OrderStatus.PENDING_MATCH); // Revert to pending match
                orderRepository.save(order);
                // Potentially publish an event like DRIVER_UNAVAILABLE_POST_ASSIGNMENT
                return;
            }

            order.setDriver(driver);
            order.setStatus(OrderStatus.DRIVER_ASSIGNED); // Or OrderStatus.ACCEPTED if driver accept is implicit with assignment
            // You might want to store ETA from event if provided
            // order.setEstimatedDriverArrivalTime(event.getEstimatedArrivalTime());
            order.setUpdatedAt(LocalDateTime.now());
            // order.setUpdatedBy(SYSTEM_USER_ID); // Or by Matching Service principal if identifiable

            orderRepository.save(order);

            // Update driver status to not available locally
            driver.setAvailable(false);
            driverRepository.save(driver);

            log.info("Driver {} successfully assigned to order {}.", event.getDriverId(), event.getOrderId());

            // Notify passenger (e.g., via Notification Service through an event)
            OrderEventDto orderStatusUpdateEvent = new OrderEventDto(
                    order.getId(), order.getPassenger().getId(), order.getDriver().getId(),
                    OrderStatus.DRIVER_ASSIGNED.name(), LocalDateTime.now(),
                    "司机已接单，正在前来", // Message for notification
                    order.getStartLatitude(), order.getStartLongitude() // For notification context
            );
            orderEventProducer.sendOrderStatusUpdateEvent(orderStatusUpdateEvent);
        }

        @Override
        @Transactional
        public void processOrderMatchFailed(Long orderId, String reason) {
            log.warn("Order match failed for order ID: {}. Reason: {}", orderId, reason);
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && (order.getStatus() == OrderStatus.PENDING_MATCH || order.getStatus() == OrderStatus.SCHEDULED)) {
                order.setStatus(OrderStatus.FAILED); // Or a more specific status like NO_DRIVER_AVAILABLE
                // order.setCancellationReason("匹配失败: " + reason); // Storing reason
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                log.info("Order ID: {} marked as FAILED due to matching failure.", orderId);

                // Notify passenger
                OrderEventDto orderFailedEvent = new OrderEventDto(
                        order.getId(), order.getPassenger().getId(), null, // No driver
                        OrderStatus.FAILED.name(), LocalDateTime.now(),
                        "很抱歉，暂时未能为您匹配到合适的司机。原因：" + reason,
                        order.getStartLatitude(), order.getStartLongitude()
                );
                orderEventProducer.sendOrderStatusUpdateEvent(orderFailedEvent);
            } else if (order != null) {
                log.warn("Order {} was not in a state for matching failure (current status: {}), ignoring match failed event.", orderId, order.getStatus());
            } else {
                log.warn("Order {} not found, ignoring match failed event.", orderId);
            }
        }

        @Autowired // Add if not present
        private OrderEventProducer orderEventProducer;


        @Override
        @Transactional
        public void processPaymentConfirmation(Long orderId, Double finalAmount, String paymentTransactionId) {
            log.info("Processing payment confirmation for order ID: {}", orderId);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("订单 " + orderId + " 未找到"));

            if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.PAYMENT_SETTLED) {

                order.setActualDropOffTime(LocalDateTime.now());
                     // Calculate actual duration based on order.getActualPickupTime() and order.getActualDropOffTime()
                     // Get actual distance if tracked, otherwise use estimate or allow driver input
                     PricingContext finalPricingContext = PricingContext.builder()
                            .startLocation(new LocationDto(order.getStartLatitude(), order.getStartLongitude()))
                            .endLocation(new LocationDto(order.getEndLatitude(), order.getEndLongitude()))
                            .vehicleType(order.getVehicleType())
                            .serviceType(order.getServiceType())
                            // .actualDistanceKm( ... ) // if available
                            // .actualDurationMinutes( ... ) // if available
                            .build();
                     FinalPriceInfo finalPrice = pricingService.calculateFinalPrice(order, finalPricingContext);
                     order.setFinalCost(finalPrice.getFinalCost());
                     order.setActualDistance(finalPrice.getActualDistance());
                     order.setActualDuration(finalPrice.getActualDuration());
                     orderRepository.save(order);
                orderRepository.save(order);

                log.info("Order ID {} status updated to PAYMENT_SETTLED. Final cost: {}", orderId, finalAmount);

                // Publish ORDER_PAYMENT_SETTLED for Analytics, Review Service etc.
                OrderPaymentSettledEvent settledEvent = new OrderPaymentSettledEvent(
                        order.getId(),
                        order.getFinalCost(),
                        order.getPaymentMethod().name(),
                        LocalDateTime.now(), // Or payment timestamp from incoming event
                        order.getPassenger().getId(),
                        order.getDriver() != null ? order.getDriver().getId() : null
                );
                orderEventProducer.sendOrderPaymentSettledEvent(settledEvent);
            } else {
                log.warn("Order {} is not in COMPLETED state to confirm payment. Current status: {}", orderId, order.getStatus());
                // This could be an issue or late message, handle as per business logic
            }
        }
}
