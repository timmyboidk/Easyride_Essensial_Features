package com.easyride.order_service.repository;
import com.easyride.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

//OrderRepository 提供了对订单实体的数据库操作方法。
//定义了根据状态、司机ID和乘客ID查询订单的方法。
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(String status);

    List<Order> findByDriverIdAndStatus(Long driverId, String status);

    List<Order> findByPassengerId(Long passengerId);
}
