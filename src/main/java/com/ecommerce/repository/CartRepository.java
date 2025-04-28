package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.cart.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Custom query methods can be defined here if needed

}
