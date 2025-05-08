package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.order.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find orders by Customer ID
    List<Order> findByCustomerUserId(Long customerId); // Assumes Customer has a userId field

    // Find order by unique order number
    Optional<Order> findByOrderNumber(String orderNumber);

    // Potentially add methods for finding orders by status, date range, etc.
    // List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findBySellerUserId(Long sellerId);

}
