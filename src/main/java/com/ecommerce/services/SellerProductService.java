package com.ecommerce.services;

import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining operations specific to a Seller managing their products.
 * Handles authorization checks related to product ownership.
 */
public interface SellerProductService {

    /**
     * Creates a new product for the specified seller.
     *
     * @param dtoProduct DTO containing product information.
     * @param sellerId   The ID of the seller creating the product.
     * @return The created product DTO.
     */
    DtoProduct createMyProduct(DtoProduct dtoProduct, Long sellerId);

    /**
     * Retrieves a paginated list of product summaries for the specified seller.
     *
     * @param sellerId The ID of the seller whose products are to be retrieved.
     * @param pageable Pagination information.
     * @return A page of DtoProductSummary objects.
     */
    Page<DtoProductSummary> getMyProducts(Long sellerId, Pageable pageable);

    /**
     * Updates an existing product owned by the specified seller.
     * Performs an ownership check before delegating to the core update logic.
     *
     * @param productId  The ID of the product to update.
     * @param dtoProduct DTO containing updated product information.
     * @param sellerId   The ID of the seller attempting the update (must match product owner).
     * @return The updated product DTO.
     * @throws org.springframework.security.access.AccessDeniedException if the seller does not own the product.
     * @throws java.util.NoSuchElementException if the product is not found.
     */
    DtoProduct updateMyProduct(Long productId, DtoProduct dtoProduct, Long sellerId);

    /**
     * Deletes a product owned by the specified seller.
     * Performs an ownership check before delegating to the core deletion logic.
     *
     * @param productId The ID of the product to delete.
     * @param sellerId  The ID of the seller attempting the deletion (must match product owner).
     * @throws org.springframework.security.access.AccessDeniedException if the seller does not own the product.
     * @throws java.util.NoSuchElementException if the product is not found.
     */
    void deleteMyProduct(Long productId, Long sellerId);

}
