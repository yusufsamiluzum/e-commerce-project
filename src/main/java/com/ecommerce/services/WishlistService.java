package com.ecommerce.services;

import com.ecommerce.dto.DtoWishlist;

/**
 * Service interface for managing customer wishlists.
 */
public interface WishlistService {

    /**
     * Retrieves the wishlist for a specific customer.
     * If the customer does not have a wishlist, one might be created implicitly
     * depending on the implementation, or an empty/default representation returned.
     *
     * @param customerId The ID of the customer.
     * @return The DtoWishlist for the customer.
     * @throws com.ecommerce.exception.ResourceNotFoundException if the customer does not exist.
     */
    DtoWishlist getWishlistForCustomer(Long customerId);

    /**
     * Adds a product to the customer's wishlist.
     *
     * @param customerId The ID of the customer.
     * @param productId The ID of the product to add.
     * @return The updated DtoWishlist.
     * @throws com.ecommerce.exception.ResourceNotFoundException if the customer or product does not exist.
     */
    DtoWishlist addProductToWishlist(Long customerId, Long productId);

    /**
     * Removes a product from the customer's wishlist.
     *
     * @param customerId The ID of the customer.
     * @param productId The ID of the product to remove.
     * @throws com.ecommerce.exception.ResourceNotFoundException if the customer or product does not exist,
     * or if the product is not in the wishlist.
     */
    void removeProductFromWishlist(Long customerId, Long productId);

}

