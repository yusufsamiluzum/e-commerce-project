package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.order.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Custom query methods can be defined here if needed

}
