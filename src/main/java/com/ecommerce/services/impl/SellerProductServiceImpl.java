package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.product.Product;
import com.ecommerce.repository.ProductRepository; // Inject repository for ownership check
import com.ecommerce.services.ProductService;     // Inject base service for core logic
import com.ecommerce.services.SellerProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException; // Standard Spring Security exception
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException; // Standard exception

@Service // Mark as a Spring service component
@RequiredArgsConstructor // Lombok for constructor injection
public class SellerProductServiceImpl implements SellerProductService {

    private final ProductService baseProductService; // Handles core CRUD logic without ownership checks
    private final ProductRepository productRepository; // Needed specifically for efficient ownership check

    @Override
    @Transactional // Inherits transactionality from base service is also possible
    public DtoProduct createMyProduct(DtoProduct dtoProduct, Long sellerId) {
        // Delegate directly to the base service method
        // Assumes baseProductService.createProduct handles setting the seller correctly
        return baseProductService.createProduct(dtoProduct, sellerId);
    }

    @Override
    @Transactional(readOnly = true) // Read-only optimization
    public Page<DtoProductSummary> getMyProducts(Long sellerId, Pageable pageable) {
        // Delegate directly to the base service method
        return baseProductService.getProductsBySeller(sellerId, pageable);
    }

    @Override
    @Transactional // Needs a transaction for potential update
    public DtoProduct updateMyProduct(Long productId, DtoProduct dtoProduct, Long sellerId) {
        // 1. Perform Authorization Check
        checkProductOwnership(productId, sellerId);

        // 2. Delegate to base service for the actual update logic
        return baseProductService.updateProduct(productId, dtoProduct);
    }

    @Override
    @Transactional // Needs a transaction for deletion
    public void deleteMyProduct(Long productId, Long sellerId) {
        // 1. Perform Authorization Check
        checkProductOwnership(productId, sellerId);

        // 2. Delegate to base service for the actual deletion logic
        baseProductService.deleteProduct(productId);
    }

    /**
     * Helper method to verify that the product exists and is owned by the expected seller.
     * Throws NoSuchElementException if the product is not found.
     * Throws AccessDeniedException if the seller does not own the product.
     *
     * @param productId        The ID of the product to check.
     * @param expectedSellerId The ID of the seller expected to own the product.
     */
    private void checkProductOwnership(Long productId, Long expectedSellerId) {
        // Fetch the product entity - ensure Seller is fetched eagerly if needed,
        // or rely on transactional context. findById might be sufficient if seller ID is on Product.
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + productId));

        // Check if the product's seller exists and their ID matches the expected ID
        // Adjust the getter according to your Seller entity (e.g., getUserId, getSellerId)
        if (product.getSeller() == null || !product.getSeller().getUserId().equals(expectedSellerId)) {
            throw new AccessDeniedException("Access Denied: Seller does not own product with id: " + productId);
        }
        // If checks pass, method completes normally
    }
}