package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoReview;
import com.ecommerce.services.ReviewService;

// Import necessary Spring annotations and classes
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Import Spring Security classes
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Or your custom UserDetails implementation
// If you have a custom UserDetails implementation (e.g., UserPrincipal) import that instead
// import com.ecommerce.security.UserPrincipal; // Example import
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST Controller for managing product reviews.
 * Provides endpoints for CRUD operations on reviews, integrated with Spring Security.
 */
@RestController
@RequestMapping("/api/v1") // Base path for all review-related endpoints
public class ReviewController {

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Creates a new review for a specific product.
     * Requires the authenticated user to have the 'CUSTOMER' role.
     * The customer ID is retrieved from the security context.
     *
     * @param productId The ID of the product being reviewed.
     * @param dtoReview The review data (rating, comment) from the request body.
     * @param authentication The Authentication object injected by Spring Security.
     * @return ResponseEntity containing the created DtoReview and HTTP status 201 (Created).
     */
    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("hasRole('CUSTOMER')") // Only users with CUSTOMER role can create reviews
    public ResponseEntity<DtoReview> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody DtoReview dtoReview,
            Authentication authentication) { // Inject Authentication object

        // --- Get Customer ID from Security Context ---
        Long customerId = getAuthenticatedUserId(authentication);
        logger.info("Authenticated customer {} attempting to create review for product {}", customerId, productId);
        // -------------------------------------------

        try {
            DtoReview createdReview = reviewService.createReview(dtoReview, customerId, productId);
            return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            logger.warn("Failed to create review for product {} by customer {}: {}", productId, customerId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
        // ResourceNotFoundException handled by @ResponseStatus
        // Other exceptions (like validation) can be handled globally
    }

    /**
     * Retrieves all reviews for a specific product.
     * Accessible to any authenticated user.
     *
     * @param productId The ID of the product.
     * @return ResponseEntity containing a list of DtoReview and HTTP status 200 (OK).
     */
    @GetMapping("/products/{productId}/reviews")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can view reviews
    public ResponseEntity<List<DtoReview>> getReviewsByProduct(@PathVariable Long productId) {
        logger.debug("Fetching reviews for product {}", productId);
        List<DtoReview> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves all reviews written by a specific customer.
     * Requires the authenticated user to have the 'ADMIN' role OR be the customer themselves.
     *
     * @param customerId The ID of the customer whose reviews are being requested.
     * @return ResponseEntity containing a list of DtoReview and HTTP status 200 (OK).
     */
    @GetMapping("/customers/{customerId}/reviews")
    // Allow ADMINs OR the customer themselves to view their reviews
    @PreAuthorize("hasRole('ADMIN') or @reviewSecurityService.isOwner(authentication, #customerId)")
    public ResponseEntity<List<DtoReview>> getReviewsByCustomer(@PathVariable Long customerId) {
        logger.debug("Fetching reviews for customer {}", customerId);
        List<DtoReview> reviews = reviewService.getReviewsByCustomerId(customerId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Retrieves a single review by its ID.
     * Accessible to any authenticated user.
     *
     * @param reviewId The ID of the review.
     * @return ResponseEntity containing the DtoReview and HTTP status 200 (OK).
     */
    @GetMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()") // Any authenticated user can view a specific review
    public ResponseEntity<DtoReview> getReviewById(@PathVariable Long reviewId) {
        logger.debug("Fetching review with id {}", reviewId);
        // ResourceNotFoundException handled by @ResponseStatus
        DtoReview review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Updates an existing review.
     * Requires the authenticated user to be the owner of the review.
     *
     * @param reviewId The ID of the review to update.
     * @param dtoReview The updated review data (rating, comment) from the request body.
     * @param authentication The Authentication object injected by Spring Security.
     * @return ResponseEntity containing the updated DtoReview and HTTP status 200 (OK).
     */
    @PutMapping("/reviews/{reviewId}")
    // Use a security service/method to check if the authenticated user owns this specific review
    @PreAuthorize("@reviewSecurityService.isReviewOwner(authentication, #reviewId)")
    public ResponseEntity<DtoReview> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody DtoReview dtoReview,
            Authentication authentication) {

        // --- Get Customer ID from Security Context ---
        Long customerId = getAuthenticatedUserId(authentication);
        logger.info("Authenticated customer {} attempting to update review {}", customerId, reviewId);
        // -------------------------------------------

        try {
            // Service layer still needs customerId for its internal check (redundant but safe)
            DtoReview updatedReview = reviewService.updateReview(reviewId, dtoReview, customerId);
            return ResponseEntity.ok(updatedReview);
        } catch (SecurityException e) {
            // This catch might become less necessary if @PreAuthorize handles it,
            // but keep for defense-in-depth or if service throws it for other reasons.
            logger.error("Authorization failed for customer {} updating review {}: {}", customerId, reviewId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
        // ResourceNotFoundException handled by @ResponseStatus
    }

    /**
     * Deletes a review.
     * Requires the authenticated user to be the owner of the review OR have the 'ADMIN' role.
     *
     * @param reviewId The ID of the review to delete.
     * @param authentication The Authentication object injected by Spring Security.
     * @return ResponseEntity with HTTP status 204 (No Content).
     */
    @DeleteMapping("/reviews/{reviewId}")
    // Allow ADMINs OR the owner of the review to delete
    @PreAuthorize("hasRole('ADMIN') or @reviewSecurityService.isReviewOwner(authentication, #reviewId)")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            Authentication authentication) {

        // --- Get User ID from Security Context (could be customer or admin) ---
        Long userId = getAuthenticatedUserId(authentication); // Use a generic name here
        logger.info("Authenticated user {} attempting to delete review {}", userId, reviewId);
        // --------------------------------------------------------------------

        try {
            // The service layer's customerId check is primarily for ownership,
            // but @PreAuthorize already handles that. We pass the userId primarily
            // for logging or if the service needs it for other checks.
            // If only the owner can delete via the service method, an Admin would need
            // a separate service method or modified logic.
            // Let's assume the service method `deleteReview` is updated to handle
            // deletion by owner OR if the caller (verified by @PreAuthorize) is an ADMIN.
            // For simplicity here, we'll call the existing method, assuming @PreAuthorize
            // provides the primary guard. A dedicated admin delete method might be cleaner.

            // **Important**: The existing `reviewService.deleteReview(reviewId, customerId)`
            // likely needs modification to work correctly with ADMIN role deletion,
            // as it currently expects the *owner's* customerId.
            // Option 1: Modify `deleteReview` to accept a flag or check role internally.
            // Option 2: Create `adminDeleteReview(reviewId)`.
            // Option 3 (Simpler for now, relies on @PreAuthorize):
            // We can retrieve the actual owner's ID if needed by the service method.
            // However, for deletion, just knowing the reviewId might be enough if
            // the authorization is done via @PreAuthorize.

            // Let's call a hypothetical modified service method for clarity:
            // reviewService.deleteReviewById(reviewId);
            // Or, if keeping the old method signature, we might need to fetch the owner ID first
            // ONLY IF the service strictly requires it even after @PreAuthorize.
            // For this example, we'll assume @PreAuthorize is sufficient and the service
            // can delete by ID after authorization passes.
             reviewService.deleteReview(reviewId, userId); // Re-evaluate this call based on service impl.

            return ResponseEntity.noContent().build(); // HTTP 204 No Content
        } catch (SecurityException e) {
            // As with PUT, this might become less necessary.
            logger.error("Authorization failed for user {} deleting review {}: {}", userId, reviewId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
        // ResourceNotFoundException handled by @ResponseStatus
    }

    /**
     * Helper method to extract the user ID from the Authentication object.
     * Adapt this based on your UserDetails implementation.
     *
     * @param authentication Spring Security Authentication object.
     * @return The authenticated user's ID.
     * @throws IllegalStateException if authentication is invalid or user ID cannot be found.
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            // Standard UserDetails usually uses username. You might need to fetch the User
            // entity from your database based on the username to get the ID.
            String username = ((UserDetails) principal).getUsername();
            // TODO: Implement logic to fetch user ID based on username
            // Example: return userRepository.findByUsername(username).orElseThrow(...).getUserId();
            // For now, placeholder - **REPLACE THIS WITH ACTUAL LOGIC**
            logger.warn("Returning placeholder user ID for username '{}'. Implement actual user ID retrieval.", username);
            // You MUST replace this with actual logic based on your User/Customer entity and repository
            // Example assuming UserDetails principal IS your User entity or a wrapper containing the ID:
            // if (principal instanceof YourUserPrincipalClass) {
            //     return ((YourUserPrincipalClass) principal).getId();
            // }
            throw new IllegalStateException("Could not determine user ID from principal. Adapt getAuthenticatedUserId method.");

        } else {
            // Handle other principal types if necessary
            throw new IllegalStateException("Unexpected principal type: " + principal.getClass());
        }
    }

    // --- Helper Security Service (Create this as a separate @Service bean) ---
    // You would need to create a bean like this:
    /*
    @Service("reviewSecurityService") // Bean name used in @PreAuthorize
    public class ReviewSecurityService {

        @Autowired
        private ReviewRepository reviewRepository; // Inject repository

        @Autowired
        private CustomerRepository customerRepository; // Or UserRepository

        // Checks if the authenticated user ID matches the requested customer ID
        public boolean isOwner(Authentication authentication, Long requestedCustomerId) {
            if (authentication == null || !authentication.isAuthenticated()) return false;
            Long authenticatedUserId = getAuthenticatedUserId(authentication); // Use helper
            return authenticatedUserId != null && authenticatedUserId.equals(requestedCustomerId);
        }

        // Checks if the authenticated user is the owner of a specific review
        public boolean isReviewOwner(Authentication authentication, Long reviewId) {
            if (authentication == null || !authentication.isAuthenticated()) return false;

            Review review = reviewRepository.findById(reviewId).orElse(null);
            if (review == null || review.getCustomer() == null) {
                return false; // Review not found or has no customer associated
            }

            Long ownerId = review.getCustomer().getUserId();
            Long authenticatedUserId = getAuthenticatedUserId(authentication); // Use helper

            return authenticatedUserId != null && authenticatedUserId.equals(ownerId);
        }

        // Copy or adapt the getAuthenticatedUserId helper method here or inject UserService
        private Long getAuthenticatedUserId(Authentication authentication) {
            // ... (Implementation similar to the one in the controller) ...
            // It's better to put this logic in a shared UserService/Component
             if (authentication == null || !authentication.isAuthenticated()) {
                 throw new IllegalStateException("User is not authenticated.");
             }
             Object principal = authentication.getPrincipal();
             if (principal instanceof UserDetails) {
                 String username = ((UserDetails) principal).getUsername();
                 // ** REPLACE WITH ACTUAL LOGIC TO GET ID FROM USERNAME **
                 // Example: return userRepository.findByUsername(username).orElseThrow(...).getUserId();
                 System.err.println("Placeholder ID logic in ReviewSecurityService for " + username);
                 return -1L; // Placeholder
             }
             throw new IllegalStateException("Unexpected principal type in ReviewSecurityService: " + principal.getClass());
        }
    }
    */
}

