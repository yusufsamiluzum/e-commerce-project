package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * Finds all reviews associated with a specific product ID.
     * @param productId The ID of the product.
     * @return A list of reviews for the given product.
     */
    List<Review> findByProductProductId(Long productId);

    /**
     * Finds all reviews written by a specific customer ID.
     * @param customerId The ID of the customer.
     * @return A list of reviews by the given customer.
     */
    List<Review> findByCustomerUserId(Long customerId);

    /**
     * Finds a review by product ID and customer ID. Useful to check if a customer
     * has already reviewed a product.
     * @param productId The ID of the product.
     * @param customerId The ID of the customer.
     * @return An Optional containing the review if found, otherwise empty.
     */
    // In ReviewRepository.java
    Optional<Review> findByProductProductIdAndCustomerUserId(Long productId, Long customerId); // Correct path

}
