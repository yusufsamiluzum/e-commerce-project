package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoCart;
import com.ecommerce.services.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
// Import your UserDetails implementation or Principal class
// import com.ecommerce.security.UserDetailsImpl;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * REST Controller for managing the customer's shopping cart.
 * Assumes customer authentication is handled by Spring Security.
 */
@RestController
@RequestMapping("/api/v1/customer/cart") // Base path for customer cart operations
@RequiredArgsConstructor // Injects CartService via constructor
@Validated // Enables validation for request parameters like quantity
public class CartController {

    private final CartService cartService;

    /**
     * Retrieves the current customer's shopping cart.
     *
     * @return ResponseEntity containing the DtoCart.
     */
    @GetMapping
    public ResponseEntity<DtoCart> getCart() {
        Long customerId = getCurrentCustomerId(); // Get ID from security context
        DtoCart cart = cartService.getCartByCustomerId(customerId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Adds an item to the current customer's shopping cart.
     *
     * @param addItemRequest DTO containing productId and quantity.
     * @return ResponseEntity containing the updated DtoCart.
     */
    @PostMapping("/items")
    public ResponseEntity<DtoCart> addItemToCart(@Valid @RequestBody DtoAddItemRequest addItemRequest) {
        Long customerId = getCurrentCustomerId();
        DtoCart updatedCart = cartService.addItemToCart(
                customerId,
                addItemRequest.getProductId(),
                addItemRequest.getQuantity()
        );
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Updates the quantity of a specific item in the current customer's cart.
     *
     * @param cartItemId The ID of the cart item to update.
     * @param quantity   The new quantity (must be positive).
     * @return ResponseEntity containing the updated DtoCart.
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<DtoCart> updateCartItemQuantity(
            @PathVariable Long cartItemId,
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
        Long customerId = getCurrentCustomerId();
        DtoCart updatedCart = cartService.updateCartItemQuantity(customerId, cartItemId, quantity);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Removes a specific item from the current customer's cart.
     *
     * @param cartItemId The ID of the cart item to remove.
     * @return ResponseEntity containing the updated DtoCart.
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<DtoCart> removeItemFromCart(@PathVariable Long cartItemId) {
        Long customerId = getCurrentCustomerId();
        DtoCart updatedCart = cartService.removeItemFromCart(customerId, cartItemId);
        return ResponseEntity.ok(updatedCart);
    }

    /**
     * Clears all items from the current customer's cart.
     *
     * @return ResponseEntity containing the empty DtoCart.
     */
    @DeleteMapping
    public ResponseEntity<DtoCart> clearCart() {
        Long customerId = getCurrentCustomerId();
        DtoCart clearedCart = cartService.clearCart(customerId);
        return ResponseEntity.ok(clearedCart);
    }

    // --- Helper Methods ---

    /**
     * Placeholder method to get the current customer's ID.
     * Replace this with your actual Spring Security logic.
     *
     * @return The authenticated customer's user ID.
     * @throws IllegalStateException if the user is not authenticated or ID cannot be retrieved.
     */
    private Long getCurrentCustomerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        // --- === Adapt this part based on your UserDetails/Principal implementation === ---
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
             // Example if using standard UserDetails and username is the ID (as String)
             // try {
             //     return Long.parseLong(((org.springframework.security.core.userdetails.UserDetails) principal).getUsername());
             // } catch (NumberFormatException e) {
             //     throw new IllegalStateException("Could not parse user ID from username", e);
             // }

             // Example if using a custom UserDetails implementation (e.g., UserDetailsImpl)
             // if (principal instanceof UserDetailsImpl) {
             //    return ((UserDetailsImpl) principal).getId(); // Assuming your UserDetailsImpl has a getId() method
             // }

             // --- === Default/Placeholder: Needs specific implementation === ---
             throw new IllegalStateException("Cannot extract customer ID from principal type: " + principal.getClass());

        } else if (principal instanceof String) {
             // Handle cases where the principal is just the username String
             // You might need to query your UserRepository here based on the username
             // throw new IllegalStateException("Principal is a String, need custom logic to get ID");
             throw new IllegalStateException("Cannot extract customer ID from principal type: String");

        } else {
            throw new IllegalStateException("Unknown principal type: " + principal.getClass());
        }

        // --- === IMPORTANT: Replace the above placeholder logic === ---
        // Example (REMOVE THIS LINE and implement above): return 1L; // Replace with actual logic
    }


    /**
     * Inner DTO class for the add item request body.
     * Includes validation constraints.
     */
    @Data // Lombok for getters, setters, toString, etc.
    static class DtoAddItemRequest {
        @NotNull(message = "Product ID cannot be null")
        private Long productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private int quantity = 1; // Default quantity to 1 if not provided
    }
}
