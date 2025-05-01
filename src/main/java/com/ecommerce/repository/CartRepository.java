package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.cart.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Custom query methods can be defined here if needed

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items ci LEFT JOIN FETCH ci.product WHERE c.customer.userId = :customerId")
    Optional<Cart> findByCustomerIdWithItems(Long customerId);

    Optional<Cart> findByCustomerUserId(Long customerId);
}
