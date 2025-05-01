package com.ecommerce.services;

import com.ecommerce.dto.DtoReview;
import java.util.List;

/**
 * Service interface for managing product reviews.
 */
public interface ReviewService {

    /**
     * Creates a new review for a product by a customer.
     * @param dtoReview The DTO containing review details (rating, comment).
     * @param customerId The ID of the customer writing the review.
     * @param productId The ID of the product being reviewed.
     * @return The created DtoReview.
     * @throws ResourceNotFoundException if the customer or product doesn't exist.
     */
    DtoReview createReview(DtoReview dtoReview, Long customerId, Long productId);

    /**
     * Retrieves all reviews for a specific product.
     * @param productId The ID of the product.
     * @return A list of DtoReview for the product.
     */
    List<DtoReview> getReviewsByProductId(Long productId);

    /**
     * Retrieves all reviews written by a specific customer.
     * @param customerId The ID of the customer.
     * @return A list of DtoReview written by the customer.
     */
    List<DtoReview> getReviewsByCustomerId(Long customerId);

    /**
     * Retrieves a single review by its ID.
     * @param reviewId The ID of the review.
     * @return The DtoReview.
     * @throws ResourceNotFoundException if the review doesn't exist.
     */
    DtoReview getReviewById(Long reviewId);

    /**
     * Updates an existing review.
     * Only the rating and comment can typically be updated.
     * @param reviewId The ID of the review to update.
     * @param dtoReview The DTO containing the updated details (rating, comment).
     * @param customerId The ID of the customer attempting the update (for authorization).
     * @return The updated DtoReview.
     * @throws ResourceNotFoundException if the review doesn't exist.
     * @throws SecurityException if the customer is not authorized to update the review.
     */
    DtoReview updateReview(Long reviewId, DtoReview dtoReview, Long customerId);

    /**
     * Deletes a review.
     * @param reviewId The ID of the review to delete.
     * @param customerId The ID of the customer attempting the deletion (for authorization).
     * @throws ResourceNotFoundException if the review doesn't exist.
     * @throws SecurityException if the customer is not authorized to delete the review.
     */
    void deleteReview(Long reviewId, Long customerId); // Or add an admin role check
}