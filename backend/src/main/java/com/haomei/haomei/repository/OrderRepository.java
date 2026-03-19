package com.haomei.haomei.repository;

import com.haomei.haomei.entity.Order;
import com.haomei.haomei.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerNameOrderByCreatedAtDesc(String customerName);
    List<Order> findByCustomerNameAndStatusOrderByCreatedAtDesc(String customerName, OrderStatus status);
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    long countByTableNoAndStatusIn(Integer tableNo, Collection<OrderStatus> statuses);
    List<Order> findByTableNoOrderByCreatedAtDesc(Integer tableNo);
}

