package com.easyride.order_service.concurrency;

import com.easyride.order_service.controller.OrderController;
import com.easyride.order_service.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConcurrencyTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void testConcurrentOrderAcceptance() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        Long orderId = 100L;

        // Simulate 10 different drivers trying to accept the same order simultaneously
        for (int i = 0; i < numberOfThreads; i++) {
            long driverId = i + 1;
            executorService.submit(() -> {
                orderController.acceptOrder(orderId, driverId);
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Verification: The controller calls service.acceptOrder 10 times.
        // The Service implementation (which is mocked here) is responsible for the
        // lock.
        // So here we verify that the controller indeed passes 10 requests through.
        // In an integration test with a real DB/Service, we'd assert only 1 succeeds.
        verify(orderService, times(numberOfThreads)).acceptOrder(eq(orderId), anyLong());
    }
}
