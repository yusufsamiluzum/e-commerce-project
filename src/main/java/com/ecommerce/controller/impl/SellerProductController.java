package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoOrderResponse;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.services.OrderService;
// Import the new service interface
import com.ecommerce.services.SellerProductService; // CHANGED
import com.ecommerce.config.securityconfig.SecurityUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST Controller for Seller-specific product management operations.
 * Requires SELLER role for access. Operations are scoped to the authenticated seller's products.
 */
@RestController
@RequestMapping("/api/v1/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerProductController {

    // Inject the new SellerProductService instead of the base ProductService
    private final SellerProductService sellerProductService; // CHANGED
    private final OrderService orderService;
    

    /**
     * POST /api/v1/seller/products : Create a new product for the authenticated seller.
     * Calls SellerProductService to handle creation.
     */
    
    
    @PostMapping // CREATE
    public ResponseEntity<DtoProduct> createMyProduct(
            Authentication authentication,
            @Valid @RequestBody DtoProduct dtoProduct) {
        try {
            Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);
            dtoProduct.setProductId(null);

            // Call the method on the injected sellerProductService
            DtoProduct createdProduct = sellerProductService.createMyProduct(dtoProduct, sellerId); // CHANGED

            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath().path("/api/v1/products/{id}")
                    .buildAndExpand(createdProduct.getProductId()).toUri();

            return ResponseEntity.created(location).body(createdProduct);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating product", e);
        }
    }

    /**
     * GET /api/v1/seller/products/my : Get products listed by the authenticated seller.
     * Calls SellerProductService to handle retrieval.
     */
    @GetMapping("/my") // READ (Seller's own)
    public ResponseEntity<Page<DtoProductSummary>> getMyProducts(Authentication authentication, Pageable pageable) {
        Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);
        // Call the method on the injected sellerProductService
        Page<DtoProductSummary> productPage = sellerProductService.getMyProducts(sellerId, pageable); // CHANGED
        return ResponseEntity.ok(productPage);
    }

    /**
     * PUT /api/v1/seller/products/{productId} : Update a product owned by the authenticated seller.
     * Calls SellerProductService to handle update and authorization.
     */
    @PutMapping("/{productId}") // UPDATE
    public ResponseEntity<DtoProduct> updateMyProduct(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody DtoProduct dtoProduct) {
        try {
            Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);

            if (dtoProduct.getProductId() != null && !dtoProduct.getProductId().equals(productId)) {
                 throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID in path must match ID in body");
            }
            dtoProduct.setProductId(productId);

            // Call the method on the injected sellerProductService
            DtoProduct updatedProduct = sellerProductService.updateMyProduct(productId, dtoProduct, sellerId); // CHANGED
            return ResponseEntity.ok(updatedProduct);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (AccessDeniedException e) { // Catch the specific exception thrown by the service
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating product", e);
        }
    }

    /**
     * DELETE /api/v1/seller/products/{productId} : Delete a product owned by the authenticated seller.
     * Calls SellerProductService to handle deletion and authorization.
     */
    @DeleteMapping("/{productId}") // DELETE
    public ResponseEntity<Void> deleteMyProduct(Authentication authentication, @PathVariable Long productId) {
        try {
            Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);
            // Call the method on the injected sellerProductService
            sellerProductService.deleteMyProduct(productId, sellerId); // CHANGED
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        } catch (AccessDeniedException e) { // Catch the specific exception thrown by the service
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting product", e);
        }
    }
}

// --- SecurityUtils Helper remains the same ---