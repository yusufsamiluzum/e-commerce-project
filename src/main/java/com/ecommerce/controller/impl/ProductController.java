package com.ecommerce.controller.impl;

import com.ecommerce.dto.DtoCategory;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.services.ProductService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST Controller for public product-related operations.
 * Exposes endpoints for browsing, searching, and viewing products.
 */
@RestController
@RequestMapping("/api/v1/products") // Base path for public product endpoints
@RequiredArgsConstructor // Injects ProductService via constructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/v1/products/filter : Filter products based on various criteria.
     * Combines search term, category, price range, etc.
     *
     * @param searchTerm Optional keyword.
     * @param categoryId Optional category ID.
     * @param minPrice   Optional minimum price.
     * @param maxPrice   Optional maximum price.
     * @param pageable   Pagination/sorting information.
     * @return ResponseEntity containing a Page of DtoProductSummary.
     */
    @GetMapping("/filter") // Renamed endpoint for clarity (optional)
    public ResponseEntity<Page<DtoProductSummary>> filterProducts(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            // Add other @RequestParam corresponding to service method parameters
            Pageable pageable) {

        Page<DtoProductSummary> results = productService.filterProducts(
                searchTerm, categoryId, minPrice, maxPrice, /* other params, */ pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/v1/products : Get a paginated list of product summaries.
     *
     * @param pageable Pagination information (e.g., ?page=0&size=10&sort=name,asc).
     * @return ResponseEntity containing a Page of DtoProductSummary.
     */
    @GetMapping
    public ResponseEntity<Page<DtoProductSummary>> getAllProducts(Pageable pageable) {
        Page<DtoProductSummary> productPage = productService.getAllProducts(pageable);
        return ResponseEntity.ok(productPage);
    }

    /**
     * GET /api/v1/products/{productId} : Get detailed information for a specific product.
     *
     * @param productId The ID of the product to retrieve.
     * @return ResponseEntity containing the DtoProduct.
     * @throws ResponseStatusException with 404 NOT_FOUND if the product doesn't exist.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<DtoProduct> getProductById(@PathVariable Long productId) {
        try {
            DtoProduct product = productService.getProductById(productId);
            return ResponseEntity.ok(product);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        }
    }

    /**
     * GET /api/v1/products/category/{categoryId} : Get products belonging to a specific category.
     *
     * @param categoryId The ID of the category.
     * @param pageable   Pagination information.
     * @return ResponseEntity containing a Page of DtoProductSummary for the category.
     * @throws ResponseStatusException with 404 NOT_FOUND if the category doesn't exist.
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<DtoProductSummary>> getProductsByCategory(
            @PathVariable Long categoryId, Pageable pageable) {
        try {
            Page<DtoProductSummary> productPage = productService.getProductsByCategory(categoryId, pageable);
            return ResponseEntity.ok(productPage);
        } catch (NoSuchElementException e) {
            // Could be thrown if the category itself doesn't exist
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found or no products in category", e);
        }
    }

    /**
     * GET /api/v1/products/seller/{sellerId} : Get products listed by a specific seller.
     *
     * @param sellerId The ID of the seller.
     * @param pageable Pagination information.
     * @return ResponseEntity containing a Page of DtoProductSummary for the seller.
     * @throws ResponseStatusException with 404 NOT_FOUND if the seller doesn't exist.
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<DtoProductSummary>> getProductsBySeller(
            @PathVariable Long sellerId, Pageable pageable) {
        try {
            Page<DtoProductSummary> productPage = productService.getProductsBySeller(sellerId, pageable);
            return ResponseEntity.ok(productPage);
        } catch (NoSuchElementException e) {
             // Could be thrown if the seller itself doesn't exist
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller not found or no products listed by seller", e);
        }
    }

    @GetMapping("/categories") // New endpoint for categories
    public ResponseEntity<List<DtoCategory>> getAllCategories() {
        List<DtoCategory> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories); // Return the list directly
    }

    // --- Placeholder for Search Endpoint ---
    /*
    @GetMapping("/search")
    public ResponseEntity<Page<DtoProductSummary>> searchProducts(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        // Assuming productService has a searchProducts method
        // Page<DtoProductSummary> results = productService.searchProducts(searchTerm, categoryId, minPrice, maxPrice, pageable);
        // return ResponseEntity.ok(results);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build(); // Example if not implemented yet
    }
    */

}
