package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.cart.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Find a specific item within a specific cart
    Optional<CartItem> findByCartCartIdAndCartItemId(Long cartId, Long cartItemId);
    // Find an item by cart ID and product ID to check if it already exists
    Optional<CartItem> findByCartCartIdAndProductProductId(Long cartId, Long productId);
}
