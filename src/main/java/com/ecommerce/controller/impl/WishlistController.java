package com.ecommerce.controller.impl;

import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.dto.DtoWishlist;
import com.ecommerce.entities.user.User;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.services.WishlistService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing customer wishlists.
 * Access is restricted to authenticated users with the 'CUSTOMER' role.
 */
@RestController
@RequestMapping("/api/wishlist") // Base path for wishlist operations
public class WishlistController {

    private static final Logger log = LoggerFactory.getLogger(WishlistController.class);

    private final WishlistService wishlistService;

    
    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * Retrieves the wishlist for the currently authenticated customer.
     *
     * @param authentication Automatically injected Authentication object.
     * @return ResponseEntity containing the DtoWishlist or an error status.
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')") // Only allow users with CUSTOMER role
    public ResponseEntity<DtoWishlist> getMyWishlist(Authentication authentication) {
        Long customerId = getAuthenticatedCustomerId(authentication);
        log.info("Fetching wishlist for authenticated customer ID: {}", customerId);
        try {
            DtoWishlist wishlist = wishlistService.getWishlistForCustomer(customerId);
            return ResponseEntity.ok(wishlist);
        } catch (ResourceNotFoundException e) {
            // This might happen if the user was deleted between authentication and this call
            log.warn("Wishlist or customer not found for authenticated customer ID: {}", customerId, e);
            // Depending on findOrCreate logic in service, this might not be reachable for customer not found
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching wishlist for customer ID: {}", customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Adds a product to the currently authenticated customer's wishlist.
     *
     * @param productId      The ID of the product to add (from path variable).
     * @param authentication Automatically injected Authentication object.
     * @return ResponseEntity containing the updated DtoWishlist or an error status.
     */
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')") // Only allow users with CUSTOMER role
    public ResponseEntity<DtoWishlist> addProductToMyWishlist(@PathVariable Long productId, Authentication authentication) {
        Long customerId = getAuthenticatedCustomerId(authentication);
        log.info("Adding product ID: {} to wishlist for authenticated customer ID: {}", productId, customerId);
        try {
            DtoWishlist updatedWishlist = wishlistService.addProductToWishlist(customerId, productId);
            return ResponseEntity.ok(updatedWishlist);
        } catch (ResourceNotFoundException e) {
            log.warn("Cannot add product to wishlist: Customer ID {} or Product ID {} not found.", customerId, productId, e);
            return ResponseEntity.notFound().build(); // 404 if customer or product doesn't exist
        } catch (Exception e) {
            log.error("Error adding product ID: {} to wishlist for customer ID: {}", productId, customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Removes a product from the currently authenticated customer's wishlist.
     *
     * @param productId      The ID of the product to remove (from path variable).
     * @param authentication Automatically injected Authentication object.
     * @return ResponseEntity with HTTP status NO_CONTENT (204) on success, or an error status.
     */
    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')") // Only allow users with CUSTOMER role
    public ResponseEntity<Void> removeProductFromMyWishlist(@PathVariable Long productId, Authentication authentication) {
        Long customerId = getAuthenticatedCustomerId(authentication);
        log.info("Removing product ID: {} from wishlist for authenticated customer ID: {}", productId, customerId);
        try {
            wishlistService.removeProductFromWishlist(customerId, productId);
            return ResponseEntity.noContent().build(); // 204 No Content on successful removal
        } catch (ResourceNotFoundException e) {
            // This exception in the service implies customer/product not found OR product not in wishlist
            log.warn("Cannot remove product from wishlist: Customer ID {}, Product ID {}, or product not in wishlist.", customerId, productId, e);
            return ResponseEntity.notFound().build(); // 404 seems appropriate here
        } catch (Exception e) {
            log.error("Error removing product ID: {} from wishlist for customer ID: {}", productId, customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to extract the customer ID from the Authentication principal.
     * Assumes the principal is an instance of UserPrincipal wrapping the User entity.
     *
     * @param authentication The Authentication object from Spring Security.
     * @return The authenticated user's ID.
     * @throws SecurityException if principal is not UserPrincipal or authentication is invalid.
     */
    private Long getAuthenticatedCustomerId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Authentication is null or not authenticated.");
            throw new SecurityException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal) {
            // Cast to UserPrincipal, get the User entity, then get the ID
            User currentUser = ((UserPrincipal) principal).getUser();
            if (currentUser == null) {
                 log.error("UserPrincipal contains a null User entity.");
                 throw new SecurityException("Authenticated principal contains no user data.");
            }
            // Assuming User entity has getUserId() (from Lombok @Data or explicit getter)
            return currentUser.getUserId();
        } else {
            log.error("Authenticated principal is not an instance of UserPrincipal. Actual type: {}",
                      principal != null ? principal.getClass().getName() : "null");
            throw new SecurityException("Unexpected principal type. Cannot extract user ID.");
        }
    }
}

