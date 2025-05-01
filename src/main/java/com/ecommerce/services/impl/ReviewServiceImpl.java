package com.ecommerce.services.impl; // Assuming this is the correct package

import com.ecommerce.dto.DtoReview;
import com.ecommerce.entities.Review;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.mappers.ReviewMapper;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.services.ReviewService;

import org.slf4j.Logger; // Added Logger
import org.slf4j.LoggerFactory; // Added LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Implementation of the ReviewService interface.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    // Logger for logging events and errors
    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    // Assuming ProductService exists to update average rating
    // private final ProductService productService;

    @Autowired
    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository
                             /*, ProductService productService */) {
        this.reviewRepository = reviewRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        // this.productService = productService;
    }

    /**
     * Creates a new review.
     * Fetches customer and product, creates the review entity, saves it,
     * and potentially updates the product's average rating.
     */
    @Override
    @Transactional // Ensure atomicity
    public DtoReview createReview(@Valid DtoReview dtoReview, Long customerId, Long productId) {
        logger.info("Attempting to create review for product {} by customer {}", productId, customerId);

        // 1. Fetch related entities using the correct exception constructor
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    String msg = String.format("Customer not found with id: %d", customerId);
                    logger.error(msg);
                    return new ResourceNotFoundException(msg);
                });
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    String msg = String.format("Product not found with id: %d", productId);
                    logger.error(msg);
                    return new ResourceNotFoundException(msg);
                });

         // 2. Optional: Check if customer already reviewed this product
         reviewRepository.findByProductProductIdAndCustomerUserId(productId, customerId).ifPresent(r -> {
             String msg = String.format("Customer %d has already reviewed product %d.", customerId, productId);
             logger.warn(msg);
             throw new IllegalStateException(msg); // Or a custom exception like DuplicateReviewException
         });   

        // 3. Create Review entity from DTO
        Review review = new Review();
        review.setCustomer(customer);
        review.setProduct(product);
        review.setRating(dtoReview.getRating()); // Validation (@Min, @Max) is on the entity
        review.setComment(dtoReview.getComment());
        // createdAt is set automatically by @CreationTimestamp

        // 4. Save the review
        Review savedReview = reviewRepository.save(review);
        logger.info("Successfully created review with id {}", savedReview.getReviewId());

        // 5. Optional: Update product's average rating and review count
        // productService.updateProductRating(productId);
        // logger.info("Triggered update for product {} rating", productId);

        // 6. Map saved entity back to DTO and return
        return ReviewMapper.toDtoReview(savedReview);
    }

    /**
     * Retrieves reviews for a specific product.
     */
    @Override
    @Transactional(readOnly = true) // Read-only transaction is efficient
    public List<DtoReview> getReviewsByProductId(Long productId) {
        logger.debug("Fetching reviews for product {}", productId);
        // Optional: Check if product exists first for a clearer error, though findBy will return empty list anyway
        if (!productRepository.existsById(productId)) {
             logger.warn("Attempted to fetch reviews for non-existent product {}", productId);
             // Decide whether to throw ResourceNotFoundException or return empty list
             // Returning empty list is often preferred for 'get all' type methods
             // throw new ResourceNotFoundException(String.format("Product not found with id: %d", productId));
        }
        List<Review> reviews = reviewRepository.findByProductProductId(productId);
        logger.debug("Found {} reviews for product {}", reviews.size(), productId);
        return ReviewMapper.toDtoReviewList(reviews);
    }

    /**
     * Retrieves reviews written by a specific customer.
     */
    @Override
    @Transactional(readOnly = true)
    public List<DtoReview> getReviewsByCustomerId(Long customerId) {
        logger.debug("Fetching reviews for customer {}", customerId);
        // Optional: Check if customer exists first
        if (!customerRepository.existsById(customerId)) {
             logger.warn("Attempted to fetch reviews for non-existent customer {}", customerId);
             // throw new ResourceNotFoundException(String.format("Customer not found with id: %d", customerId));
        }
        List<Review> reviews = reviewRepository.findByCustomerUserId(customerId);
        logger.debug("Found {} reviews for customer {}", reviews.size(), customerId);
        return ReviewMapper.toDtoReviewList(reviews);
    }

    /**
     * Retrieves a single review by its ID.
     */
    @Override
    @Transactional(readOnly = true)
    public DtoReview getReviewById(Long reviewId) {
        logger.debug("Fetching review with id {}", reviewId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    String msg = String.format("Review not found with id: %d", reviewId);
                    logger.error(msg);
                    return new ResourceNotFoundException(msg);
                });
        return ReviewMapper.toDtoReview(review);
    }

    /**
     * Updates an existing review.
     * Ensures the user attempting the update is the one who wrote the review.
     */
    @Override
    @Transactional
    public DtoReview updateReview(Long reviewId, @Valid DtoReview dtoReview, Long customerId) {
        logger.info("Attempting to update review {} by customer {}", reviewId, customerId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                     String msg = String.format("Review not found with id: %d for update attempt", reviewId);
                     logger.error(msg);
                     return new ResourceNotFoundException(msg);
                 });

        // Authorization check: Ensure the customer owns the review
        if (!review.getCustomer().getUserId().equals(customerId)) {
            String msg = String.format("Customer %d is not authorized to update review %d owned by customer %d.",
                    customerId, reviewId, review.getCustomer().getUserId());
            logger.error(msg);
            // Consider a more specific exception like AccessDeniedException or ForbiddenStatusException
            throw new SecurityException("Customer is not authorized to update this review.");
        }

        // Update allowed fields
        boolean updated = false;
        if (review.getRating() != dtoReview.getRating()) {
            review.setRating(dtoReview.getRating());
            updated = true;
        }
        if (dtoReview.getComment() != null && !dtoReview.getComment().equals(review.getComment())) {
            review.setComment(dtoReview.getComment());
            updated = true;
        }

        if (!updated) {
            logger.info("No changes detected for review {}. Returning existing data.", reviewId);
            return ReviewMapper.toDtoReview(review); // Avoid unnecessary save if no changes
        }

        // Note: createdAt should not be updated. Add an updatedAt field to Review entity if needed.
        // review.setUpdatedAt(LocalDateTime.now()); // If you add an updatedAt field

        Review updatedReview = reviewRepository.save(review);
        logger.info("Successfully updated review {}", reviewId);

        // Optional: Recalculate product's average rating after update
        // productService.updateProductRating(review.getProduct().getProductId());
        // logger.info("Triggered update for product {} rating after review update", review.getProduct().getProductId());

        return ReviewMapper.toDtoReview(updatedReview);
    }

    /**
     * Deletes a review.
     * Ensures the user attempting the deletion is the one who wrote the review
     * (or potentially an admin).
     */
    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long customerId) {
        logger.info("Attempting to delete review {} by customer {}", reviewId, customerId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> {
                    String msg = String.format("Review not found with id: %d for delete attempt", reviewId);
                    logger.error(msg);
                    return new ResourceNotFoundException(msg);
                });

        // Authorization check
        // TODO: Add logic here to allow Admins to delete reviews as well (e.g., check user roles)
        // Example: if (!review.getCustomer().getUserId().equals(customerId) && !currentUser.isAdmin()) { ... }
        if (!review.getCustomer().getUserId().equals(customerId)) {
             String msg = String.format("Customer %d is not authorized to delete review %d owned by customer %d.",
                    customerId, reviewId, review.getCustomer().getUserId());
            logger.error(msg);
            throw new SecurityException("Customer is not authorized to delete this review.");
        }

        Long productId = review.getProduct().getProductId(); // Get product ID before deleting

        reviewRepository.delete(review);
        logger.info("Successfully deleted review {}", reviewId);

        // Optional: Update product's average rating after deletion
        // productService.updateProductRating(productId);
        // logger.info("Triggered update for product {} rating after review deletion", productId);
    }
}

