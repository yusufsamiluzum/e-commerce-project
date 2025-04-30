package com.ecommerce.services;

// Import user-provided DTOs
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
// Removed custom exception import
// import com.ecommerce.exception.ResourceNotFoundException;

import org.springframework.data.domain.Page; // For pagination
import org.springframework.data.domain.Pageable; // For pagination



/**
 * Service interface for managing Products using specific DTOs.
 * Defines the contract for CRUD operations and other product-related business logic.
 */
public interface ProductService {

    /**
     * Creates a new product associated with a specific seller.
     * Accepts the full product details via DtoProduct.
     *
     * @param dtoProduct DTO containing the details of the product to create.
     * Note: Fields like productId, averageRating, reviewCount, seller,
     * approval status, and timestamps are typically ignored on creation
     * as they are set by the system or other processes.
     * @param sellerId   The ID of the seller creating the product.
     * @return DtoProduct representing the newly created product with system-assigned values.
     * @throws RuntimeException if the seller or specified categories do not exist.
     */
    DtoProduct createProduct(DtoProduct dtoProduct, Long sellerId);

    /**
     * Retrieves a product by its ID, returning full details.
     *
     * @param productId The ID of the product to retrieve.
     * @return DtoProduct representing the found product.
     * @throws RuntimeException if the product with the given ID is not found.
     */
    DtoProduct getProductById(Long productId);

    /**
     * Retrieves a paginated list of all products, returning summaries.
     *
     * @param pageable Pagination information (page number, size, sort).
     * @return A Page of DtoProductSummary objects.
     */
    Page<DtoProductSummary> getAllProducts(Pageable pageable);

    /**
     * Retrieves a paginated list of all products listed by a specific seller, returning summaries.
     *
     * @param sellerId The ID of the seller.
     * @param pageable Pagination information.
     * @return A Page of DtoProductSummary objects for the given seller.
     * @throws RuntimeException if the seller with the given ID is not found.
     */
    Page<DtoProductSummary> getProductsBySeller(Long sellerId, Pageable pageable);

    /**
     * Retrieves a paginated list of all products belonging to a specific category, returning summaries.
     *
     * @param categoryId The ID of the category.
     * @param pageable   Pagination information.
     * @return A Page of DtoProductSummary objects for the given category.
     * @throws RuntimeException if the category with the given ID is not found.
     */
    Page<DtoProductSummary> getProductsByCategory(Long categoryId, Pageable pageable);


    /**
     * Updates an existing product.
     * Accepts the full product details via DtoProduct.
     *
     * @param productId  The ID of the product to update.
     * @param dtoProduct DTO containing the updated details.
     * Note: Fields like productId, seller, approval status, and timestamps
     * are generally not updatable via this method or handled internally.
     * @return DtoProduct representing the updated product.
     * @throws RuntimeException if the product or specified categories do not exist.
     * @// TODO: Add authorization check to ensure only the seller or admin can update.
     */
    DtoProduct updateProduct(Long productId, DtoProduct dtoProduct);

    /**
     * Deletes a product by its ID.
     *
     * @param productId The ID of the product to delete.
     * @throws RuntimeException if the product with the given ID is not found (optional, could also just do nothing if not found).
     * @// TODO: Add authorization check.
     */
    void deleteProduct(Long productId);

    /**
     * Approves a product (typically done by an Admin).
     *
     * @param productId The ID of the product to approve.
     * @return DtoProduct representing the approved product (with updated approval status/timestamp).
     * @throws RuntimeException if the product with the given ID is not found.
     * @// TODO: Add authorization check (Admin only).
     */
    DtoProduct approveProduct(Long productId);

    // --- Consider adding search/filtering methods ---
    /**
     * Searches for products based on various criteria (e.g., name, description, brand, attributes).
     *
     * @param searchTerm Search query string.
     * @param categoryId Optional category filter.
     * @param minPrice Optional minimum price filter.
     * @param maxPrice Optional maximum price filter.
     * @param pageable Pagination information.
     * @return A Page of DtoProductSummary objects matching the criteria.
     */
    // Page<DtoProductSummary> searchProducts(String searchTerm, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);


    // --- Methods for managing specific associations if needed ---
    // DtoProduct addProductImage(Long productId, DtoProductImage imageDto);
    // void deleteProductImage(Long productId, Long imageId);
    // DtoProduct addProductVariant(Long productId, DtoVariant variantDto);
    // void deleteProductVariant(Long productId, Long variantId);

}


