package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.product.Category;
import com.ecommerce.entities.product.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds a product by its ID and eagerly fetches associated Seller and Categories.
     * This helps prevent N+1 query issues when loading product details.
     * Other associations like images, variants, attributes might still be loaded lazily
     * or fetched explicitly in the service layer depending on mapping needs.
     *
     * @param productId The ID of the product.
     * @return An Optional containing the Product with fetched associations, or empty if not found.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.seller LEFT JOIN FETCH p.categories WHERE p.productId = :productId")
    Optional<Product> findProductWithAssociationsById(@Param("productId") Long productId);

    /**
     * Finds all products sold by a specific seller, supporting pagination.
     *
     * @param sellerId The ID of the seller.
     * @param pageable Pagination information (page, size, sort).
     * @return A Page of Products sold by the specified seller.
     */
    Page<Product> findBySellerSellerId(Long sellerId, org.springframework.data.domain.Pageable pageable);

    /**
     * Finds all products belonging to a specific category, supporting pagination.
     *
     * @param category The Category entity.
     * @param pageable Pagination information (page, size, sort).
     * @return A Page of Products belonging to the specified category.
     */
    Page<Product> findByCategoriesContains(Category category, org.springframework.data.domain.Pageable pageable);

    // Note: JpaRepository already provides:
    // - Optional<Product> findById(Long productId)
    // - Page<Product> findAll(Pageable pageable)
    // - Product save(Product product)
    // - void deleteById(Long productId)
    // - boolean existsById(Long productId)
    // ... and other standard methods.

    // You might add more custom queries here later, for example, for searching:
    /*
    @Query("SELECT p FROM Product p WHERE " +
           "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:categoryId IS NULL OR :categoryId MEMBER OF p.categories) AND " + // Note: MEMBER OF might require category object, adjust query if needed
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> searchProducts(
        @Param("searchTerm") String searchTerm,
        @Param("categoryId") Long categoryId, // Or Category category
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );
    */

}
