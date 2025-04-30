package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.product.ProductAttribute;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {
    // Custom query methods can be defined here if needed
    // For example, find by product ID, etc.

    List<ProductAttribute> findByVariantVariantId(Long variantId);
    void deleteByVariantVariantId(Long variantId); // Ensure transactional safety
    void deleteByProductProductId(Long productId); // Potentially needed if attributes can link directly to product
}
