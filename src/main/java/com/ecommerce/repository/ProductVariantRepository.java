package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.product.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    // Custom query methods can be defined here if needed
    // For example, find by product ID, etc.

    List<ProductVariant> findByProductProductId(Long productId);
    void deleteByProductProductId(Long productId); // Ensure transactional safety
}
