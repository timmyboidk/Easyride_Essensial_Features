package com.easyride.order_service.repository;

import com.easyride.order_service.model.Order;
import com.easyride.order_service.model.OrderStatus;
import com.easyride.order_service.model.Passenger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
/**
 * 【最终关键修复】: 使用 @TestPropertySource 强制覆盖配置
 * 这个注解提供了比任何 application.yml 或 .properties 文件都高的配置优先级。
 * 我们在这里用它来强制指定测试环境必须遵守的行为：
 * 1. spring.jpa.hibernate.ddl-auto=create-drop: 强制Hibernate在测试开始时创建所有表，在测试结束时删除它们。
 * 2. spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect:
 * 我们使用和 application.yml 完全相同的属性键，来直接覆盖它的值，强制Hibernate使用H2方言。
 * 这将从根源上解决所有由于方言不匹配导致的建表失败问题。
 */
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void findByStatus_whenOrdersWithStatusExist_shouldReturnOrderList() {
        // 1. 准备 (Arrange)
        Passenger passenger = new Passenger();
        passenger.setUsername("Test Passenger");
        entityManager.persist(passenger);

        Order order1 = new Order();
        order1.setStatus(OrderStatus.COMPLETED);
        order1.setPassenger(passenger);
        entityManager.persist(order1);

        Order order2 = new Order();
        order2.setStatus(OrderStatus.COMPLETED);
        order2.setPassenger(passenger);
        entityManager.persist(order2);

        Order order3 = new Order();
        order3.setStatus(OrderStatus.CANCELED);
        order3.setPassenger(passenger);
        entityManager.persist(order3);

        entityManager.flush();

        // 2. 执行 (Act)
        List<Order> foundOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        // 3. 断言 (Assert)
        assertThat(foundOrders)
                .isNotNull()
                .hasSize(2)
                .containsExactlyInAnyOrder(order1, order2);
    }

    @Test
    void findByStatus_whenNoOrderWithStatusExists_shouldReturnEmptyList() {
        // 1. 准备 (Arrange)
        Passenger passenger = new Passenger();
        passenger.setUsername("Another Passenger");
        entityManager.persist(passenger);

        Order order = new Order();
        order.setStatus(OrderStatus.CANCELED);
        order.setPassenger(passenger);
        entityManager.persistAndFlush(order);

        // 2. 执行 (Act)
        List<Order> foundOrders = orderRepository.findByStatus(OrderStatus.COMPLETED);

        // 3. 断言 (Assert)
        assertThat(foundOrders)
                .isNotNull()
                .isEmpty();
    }
}