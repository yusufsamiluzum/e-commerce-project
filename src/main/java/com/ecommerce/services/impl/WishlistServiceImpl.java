package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoWishlist;
import com.ecommerce.entities.Wishlist;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer; // Import Customer specifically
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.mappers.WishlistMapper;
import com.ecommerce.repository.CustomerRepository; // Assuming you have a CustomerRepository
import com.ecommerce.repository.ProductRepository; // Assuming you have a ProductRepository
import com.ecommerce.repository.WishlistRepository;
import com.ecommerce.services.WishlistService;

import jakarta.transaction.Transactional; // Use jakarta.transactional
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.util.HashSet; // Import HashSet

/**
 * Implementation of the WishlistService interface.
 */
@Service
public class WishlistServiceImpl implements WishlistService {

    private static final Logger log = LoggerFactory.getLogger(WishlistServiceImpl.class);

    private final WishlistRepository wishlistRepository;
    private final CustomerRepository customerRepository; // Use CustomerRepository
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

   
    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               CustomerRepository customerRepository, // Inject CustomerRepository
                               ProductRepository productRepository,
                               WishlistMapper wishlistMapper) {
        this.wishlistRepository = wishlistRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.wishlistMapper = wishlistMapper;
    }

    /**
     * Finds or creates a wishlist for a given customer.
     *
     * @param customerId The ID of the customer.
     * @return The Wishlist entity.
     * @throws ResourceNotFoundException if the customer doesn't exist.
     */
    private Wishlist findOrCreateWishlistByCustomerId(Long customerId) {
        // Find the customer first
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Try to find existing wishlist
        return wishlistRepository.findByUser(customer)
                .orElseGet(() -> {
                    // If not found, create a new one
                    log.info("No wishlist found for customer ID: {}. Creating a new one.", customerId);
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUser(customer);
                    // Initialize the product set if it's null (though the entity definition does this)
                    if (newWishlist.getProducts() == null) {
                         newWishlist.setProducts(new HashSet<>());
                    }
                    return wishlistRepository.save(newWishlist);
                });
    }

    /**
     * Retrieves the wishlist DTO for a specific customer.
     */
    @Override
    @Transactional // Read-only is often sufficient, but findOrCreate might save
    public DtoWishlist getWishlistForCustomer(Long customerId) {
        log.debug("Attempting to get wishlist for customer ID: {}", customerId);
        Wishlist wishlist = findOrCreateWishlistByCustomerId(customerId);
        log.info("Retrieved wishlist ID: {} for customer ID: {}", wishlist.getWishlistId(), customerId);
        return wishlistMapper.toWishlistDto(wishlist);
    }

    /**
     * Adds a product to the customer's wishlist.
     */
    @Override
    @Transactional // This operation modifies data
    public DtoWishlist addProductToWishlist(Long customerId, Long productId) {
        log.debug("Attempting to add product ID: {} to wishlist for customer ID: {}", productId, customerId);

        Wishlist wishlist = findOrCreateWishlistByCustomerId(customerId); // Ensures customer exists and gets/creates wishlist

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Add product to the set (Set handles duplicates automatically)
        boolean added = wishlist.getProducts().add(product);

        if (added) {
            log.info("Product ID: {} added to wishlist ID: {}", productId, wishlist.getWishlistId());
            wishlistRepository.save(wishlist); // Save the changes
        } else {
            log.info("Product ID: {} was already in wishlist ID: {}", productId, wishlist.getWishlistId());
        }

        // Return the updated wishlist DTO
        return wishlistMapper.toWishlistDto(wishlist);
    }

    /**
     * Removes a product from the customer's wishlist.
     */
    @Override
    @Transactional // This operation modifies data
    public void removeProductFromWishlist(Long customerId, Long productId) {
        log.debug("Attempting to remove product ID: {} from wishlist for customer ID: {}", productId, customerId);

        // Find the customer first to ensure they exist
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Find the wishlist - it *must* exist if the customer exists and we used findOrCreate before,
        // but we check anyway or handle cases where it might not (e.g., direct DB manipulation)
        Wishlist wishlist = wishlistRepository.findByUser(customer)
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist", "customerId", customerId));
                // Or potentially log a warning and do nothing if wishlist doesn't exist

        // Find the product to remove
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Remove the product from the set
        boolean removed = wishlist.getProducts().remove(product);

        if (removed) {
            log.info("Product ID: {} removed from wishlist ID: {}", productId, wishlist.getWishlistId());
            wishlistRepository.save(wishlist); // Save the changes
        } else {
            // Product wasn't in the wishlist, throw an exception or log?
            // Throwing makes the API contract clearer.
            log.warn("Attempted to remove Product ID: {} from Wishlist ID: {}, but it was not found.", productId, wishlist.getWishlistId());
            throw new ResourceNotFoundException("Product", "id " + productId + " in Wishlist", wishlist.getWishlistId());
             // Or simply log and return void if non-existence isn't an error state
        }
    }
}

