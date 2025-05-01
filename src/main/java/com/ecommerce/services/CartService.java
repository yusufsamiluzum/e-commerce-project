// src/main/java/com/ecommerce/services/CartService.java
package com.ecommerce.services;

import com.ecommerce.dto.DtoCart; //

public interface CartService {

    /**
     * Retrieves the cart for the specified customer. Creates one if it doesn't exist.
     *
     * @param customerId The ID of the customer. //
     * @return The customer's cart DTO. //
     */
    DtoCart getCartByCustomerId(Long customerId);

    /**
     * Adds a product to the customer's cart or updates the quantity if it already exists.
     *
     * @param customerId The ID of the customer. //
     * @param productId  The ID of the product to add. //
     * @param quantity   The quantity to add.
     * @return The updated cart DTO. //
     */
    DtoCart addItemToCart(Long customerId, Long productId, int quantity);

    /**
     * Updates the quantity of a specific item in the customer's cart.
     *
     * @param customerId The ID of the customer. //
     * @param cartItemId The ID of the cart item to update. //
     * @param quantity   The new quantity (must be > 0).
     * @return The updated cart DTO. //
     */
    DtoCart updateCartItemQuantity(Long customerId, Long cartItemId, int quantity);

    /**
     * Removes a specific item from the customer's cart.
     *
     * @param customerId The ID of the customer. //
     * @param cartItemId The ID of the cart item to remove. //
     * @return The updated cart DTO. //
     */
    DtoCart removeItemFromCart(Long customerId, Long cartItemId);

    /**
     * Removes all items from the customer's cart.
     *
     * @param customerId The ID of the customer. //
     * @return The empty cart DTO. //
     */
    DtoCart clearCart(Long customerId);
}
