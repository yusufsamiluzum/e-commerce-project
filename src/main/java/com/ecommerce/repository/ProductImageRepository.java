package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.product.ProductImage;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    // Custom query methods can be defined here if needed
    // For example, find by product ID, etc.


    List<ProductImage> findByProductProductId(Long productId);
    Optional<ProductImage> findByProductProductIdAndIsPrimary(Long productId, boolean isPrimary);
    Optional<ProductImage> findFirstByProductProductIdOrderByImageIdAsc(Long productId);
    void deleteByProductProductId(Long productId); // Ensure transactional safety
}
