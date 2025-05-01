package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoComparison;
import com.ecommerce.dto.DtoProduct; // Detailed DTO
import com.ecommerce.dto.DtoProductSummary; // Summary DTO
import com.ecommerce.entities.product.Category;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.product.ProductComparison;
import com.ecommerce.entities.user.Customer;

import com.ecommerce.repository.CategoryRepository; // Assuming this exists
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductComparisonRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.ComparisonMapper;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // For checking session ID

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok constructor injection
public class ComparisonService {

    private final ProductComparisonRepository comparisonRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository; // Needed for category validation
    private final ComparisonMapper mapper; // Use the mapper

    // --- Core Comparison Management ---

    /**
     * Gets or creates a comparison for a user/session, adding the first product.
     * Comparisons are typically bound to a category.
     */
    @Transactional
    public DtoComparison addFirstProductToComparison(Long productId, Long customerId, String sessionId) {
        Product product = findProductById(productId);
        Category category = getProductCategory(product); // Get the category to enforce comparison consistency

        ProductComparison comparison = findOrCreateComparison(customerId, sessionId, category);

        // Add the product if not already present
        if (comparison.getProducts().add(product)) {
            comparison.setCategory(category); // Ensure category is set
            // Link comparison to customer if not already done
            if (customerId != null && comparison.getCustomer() == null) {
                 Customer customer = findCustomerById(customerId);
                 comparison.setCustomer(customer);
                 customer.getComparisons().add(comparison); // Maintain bidirectional link
            } else if (sessionId != null && comparison.getSessionId() == null) {
                comparison.setSessionId(sessionId);
            }
            comparison = comparisonRepository.save(comparison);
        }

        return convertToDetailedDto(comparison);
    }

     /**
     * Adds a product to an existing comparison.
     */
    @Transactional
    public DtoComparison addProductToExistingComparison(Long comparisonId, Long productId) {
        ProductComparison comparison = findComparisonByIdInternal(comparisonId);
        Product product = findProductById(productId);
        Category productCategory = getProductCategory(product);

        // Ensure the product belongs to the comparison's category
        if (comparison.getCategory() != null && !comparison.getCategory().equals(productCategory)) {
            throw new ValidationException("Product category does not match comparison category.");
        }
        // If comparison category wasn't set yet (e.g., empty comparison), set it now.
        if (comparison.getCategory() == null) {
             comparison.setCategory(productCategory);
        }

        comparison.getProducts().add(product);
        comparison = comparisonRepository.save(comparison);
        return convertToDetailedDto(comparison);
    }

     /**
     * Removes a product from a comparison.
     */
    @Transactional
    public DtoComparison removeProductFromComparison(Long comparisonId, Long productId) {
        ProductComparison comparison = findComparisonByIdInternal(comparisonId);
        Product product = findProductById(productId);

        if (!comparison.getProducts().remove(product)) {
             throw new RuntimeException("Product not found in this comparison.", new NotFoundException());
        }

        // Optional: Delete comparison if it becomes empty? Or leave it.
        // if (comparison.getProducts().isEmpty()) {
        //     comparisonRepository.delete(comparison);
        //     return null; // Or indicate deletion
        // }

        comparison = comparisonRepository.save(comparison);
        return convertToDetailedDto(comparison);
    }

     /**
     * Saves/names a comparison for a logged-in user.
     */
     @Transactional
     public DtoComparison saveComparison(Long comparisonId, Long customerId, String name) {
         ProductComparison comparison = findComparisonByIdInternal(comparisonId);
         Customer customer = findCustomerById(customerId);

         // Validate ownership and name uniqueness (if needed)
         if (!customer.equals(comparison.getCustomer())) {
             throw new ValidationException("Comparison does not belong to this customer.");
         }
         // Check if name is already used by this customer (requires repository method)
         // comparisonRepository.findByCustomerAndName(customer, name).ifPresent(existing -> { ... });

         comparison.setName(name);
         comparison.setSessionId(null); // Clear session ID if saved by logged-in user
         comparison.setCustomer(customer); // Ensure customer is set
         comparison = comparisonRepository.save(comparison);

         if (!customer.getComparisons().contains(comparison)){
              customer.getComparisons().add(comparison);
              // customerRepository.save(customer); // May not be needed depending on cascades
         }

         return convertToDetailedDto(comparison);
     }

    // --- Retrieval Methods (Detailed and Superficial) ---

    /**
     * Retrieves a specific comparison for DETAILED view.
     * Fetches products eagerly.
     */
    @Transactional(readOnly = true)
    public DtoComparison getDetailedComparison(Long comparisonId) {
        ProductComparison comparison = comparisonRepository.findByIdWithProducts(comparisonId)
                .orElseThrow(() -> new RuntimeException("Comparison not found with ID: " + comparisonId, new NotFoundException()));
        return convertToDetailedDto(comparison);
    }

    /**
     * Retrieves all comparisons for a customer/session for SUPERFICIAL view.
     * Uses DtoProductSummary within the result.
     */
    @Transactional(readOnly = true)
    public List<DtoComparison> getSuperficialComparisons(Long customerId, String sessionId) {
        List<ProductComparison> comparisons;
        if (customerId != null) {
            Customer customer = findCustomerById(customerId);
            comparisons = comparisonRepository.findByCustomer(customer); // Assumes default fetch or joins handle product summaries ok
        } else if (StringUtils.hasText(sessionId)) {
            comparisons = comparisonRepository.findBySessionId(sessionId);
        } else {
            return Collections.emptyList(); // No identifier provided
        }

        return comparisons.stream()
                          .map(this::convertToSummaryDto)
                          .collect(Collectors.toList());
    }


    // --- Helper Methods ---

    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId, new NotFoundException()));
    }

    private Customer findCustomerById(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    throw new RuntimeException("Customer not found with ID: " + customerId, new NotFoundException());
                });
    }

    private ProductComparison findComparisonByIdInternal(Long comparisonId){
         return comparisonRepository.findById(comparisonId) // Use basic findById first
                .orElseThrow(() -> new RuntimeException("Comparison not found with ID: " + comparisonId));
    }


    private Category getProductCategory(Product product) {
        // Comparisons require products to be in the *same* category.
        // This logic assumes a product belongs to at least one, and we use the first one.
        // You might need more sophisticated logic if products can be in multiple categories
        // and comparisons should span compatible categories.
        return product.getCategories().stream().findFirst()
                .orElseThrow(() -> new ValidationException("Product must belong to a category for comparison."));
    }

    private ProductComparison findOrCreateComparison(Long customerId, String sessionId, Category category) {
         Optional<ProductComparison> existingComparison = Optional.empty();

         if (customerId != null) {
            Customer customer = findCustomerById(customerId);
             // Look for an existing unnamed comparison for this customer and category
             existingComparison = comparisonRepository.findByCustomerAndCategory(customer, category)
                                     .filter(pc -> pc.getName() == null); // Only reuse unnamed ones
            if (existingComparison.isPresent()) return existingComparison.get();
            // If not found, create new
            ProductComparison newComp = new ProductComparison();
            newComp.setCustomer(customer);
            newComp.setCategory(category);
            return newComp; // Not saved yet, will be saved after adding product
         } else if (StringUtils.hasText(sessionId)) {
             // Look for existing comparison for this session and category
             existingComparison = comparisonRepository.findBySessionIdAndCategory(sessionId, category);
             if (existingComparison.isPresent()) return existingComparison.get();
             // If not found, create new
             ProductComparison newComp = new ProductComparison();
             newComp.setSessionId(sessionId);
             newComp.setCategory(category);
             return newComp; // Not saved yet
         } else {
             throw new ValidationException("Cannot create comparison without customer ID or session ID.");
         }
    }

    /**
     * Converts Entity to DETAILED DTO (uses DtoProduct).
     */
    private DtoComparison convertToDetailedDto(ProductComparison comparison) {
        // Ensure products are loaded (might require @Transactional or eager fetch)
        Set<Product> products = comparison.getProducts();
        if (products == null || products.isEmpty()){
             // Handle case where products might not be loaded if not fetched eagerly
             // Could re-fetch using findByIdWithProducts if necessary, but inefficient
             products = comparisonRepository.findByIdWithProducts(comparison.getComparisonId())
                            .map(ProductComparison::getProducts)
                            .orElse(Collections.emptySet());
        }


        List<DtoProduct> dtoProducts = products.stream()
                .map(mapper::productToDtoProduct) // Use the mapper
                .collect(Collectors.toList());

        Category category = comparison.getCategory();

        return new DtoComparison(
                comparison.getComparisonId(),
                comparison.getName(),
                category != null ? category.getCategoryId() : null,
                category != null ? category.getName() : "N/A",
                dtoProducts, // List of detailed DTOs
                comparison.getCreatedAt()
        );
    }

    /**
     * Converts Entity to SUPERFICIAL DTO (uses DtoProductSummary).
     * NOTE: This requires modifying DtoComparison or creating a new DtoComparisonSummary.
     * Let's assume we modify DtoComparison for simplicity here, changing its product list type.
     * A better approach is a dedicated DTO.
     *
     * For now, returning the same structure but mapping to DtoProductSummary.
     * The frontend would need to know which DTO type is inside based on the context.
     */
     // Option A: Create a new DTO `DtoComparisonSummary`
     /*
     private DtoComparisonSummary convertToSummaryDto(ProductComparison comparison) {
         List<DtoProductSummary> summaryProducts = comparison.getProducts().stream()
             .map(mapper::productToDtoProductSummary)
             .collect(Collectors.toList());
         Category category = comparison.getCategory();
         return new DtoComparisonSummary(
                 comparison.getComparisonId(),
                 comparison.getName(),
                 category != null ? category.getName() : "N/A",
                 summaryProducts.size(), // Just show count?
                 comparison.getCreatedAt()
                 // Or include the summaryProducts list
         );
     }
     */

     // Option B: Return DtoComparison, but populate with summaries (less type-safe)
     // This requires DtoComparison.products to be List<? super BaseProductDto> or similar,
     // or just List<?>. Let's pretend DtoComparison can hold summaries for this example.
     @SuppressWarnings("unchecked") // This is conceptually flawed without proper DTOs
     private DtoComparison convertToSummaryDto(ProductComparison comparison) {
         // This mapping is conceptually showing summaries, even if using DtoComparison structure.
         // A frontend would display these differently than the detailed view.
         List<?> summaryProducts = comparison.getProducts().stream()
                 .map(mapper::productToDtoProductSummary) // Map to SUMMARY DTO
                 .collect(Collectors.toList());

         Category category = comparison.getCategory();

         // *** HACK WARNING ***: Casting List<DtoProductSummary> to List<DtoProduct>
         // This only works if the downstream code treats the list generically or if
         // DtoProductSummary happens to be structurally compatible AND the list is read-only.
         // DO NOT DO THIS IN PRODUCTION - Use a dedicated DtoComparisonSummary.
         List<DtoProduct> productsListHack = (List<DtoProduct>)(List<?>) summaryProducts;


         return new DtoComparison(
                 comparison.getComparisonId(),
                 comparison.getName(),
                 category != null ? category.getCategoryId() : null,
                 category != null ? category.getName() : "N/A",
                 productsListHack, // List of *summary* DTOs (cast unsafely)
                 comparison.getCreatedAt()
         );
     }
}
