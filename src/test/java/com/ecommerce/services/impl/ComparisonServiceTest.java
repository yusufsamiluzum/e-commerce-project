package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoComparison;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.product.Category;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.product.ProductComparison;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.mappers.ComparisonMapper;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductComparisonRepository;
import com.ecommerce.repository.ProductRepository;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @Mock
    private ProductComparisonRepository comparisonRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository; // Although not directly used in many methods, it's a dependency
    @Mock
    private ComparisonMapper mapper;

    @InjectMocks
    private ComparisonService comparisonService;

    private Product product1;
    private Product product2;
    private Customer customer;
    private Category category1;
    private Category category2;
    private ProductComparison comparison;
    private DtoProduct dtoProduct1;
    private DtoProductSummary dtoProductSummary1;


    @BeforeEach
    void setUp() {
        // --- Entities ---
        category1 = new Category();
        category1.setCategoryId(1L);
        category1.setName("Electronics");

        category2 = new Category();
        category2.setCategoryId(2L);
        category2.setName("Books");

        product1 = new Product();
        product1.setProductId(101L);
        product1.setName("Laptop X");
        product1.setPrice(BigDecimal.valueOf(1200.00));
        product1.setCategories(Set.of(category1)); // Product belongs to category1
        product1.setBrand("BrandA");
        product1.setModel("ModelX");


        product2 = new Product();
        product2.setProductId(102L);
        product2.setName("Smartphone Y");
        product2.setPrice(BigDecimal.valueOf(800.00));
        product2.setCategories(Set.of(category1)); // Product belongs to category1
         product2.setBrand("BrandB");
        product2.setModel("ModelY");

        customer = new Customer();
        customer.setUserId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setComparisons(new ArrayList<>()); // Initialize collections

        comparison = new ProductComparison();
        comparison.setComparisonId(50L);
        comparison.setCustomer(customer);
        comparison.setCategory(category1);
        comparison.setProducts(new HashSet<>(Set.of(product1))); // Start with one product
        comparison.setCreatedAt(LocalDateTime.now().minusDays(1));

        // --- DTOs ---
         dtoProduct1 = new DtoProduct(); // Assume mapper creates this structure
         dtoProduct1.setProductId(101L);
         dtoProduct1.setName("Laptop X");
         dtoProduct1.setPrice(BigDecimal.valueOf(1200.00));

         dtoProductSummary1 = new DtoProductSummary(101L, "Laptop X", BigDecimal.valueOf(1200.00), "img_url", 4.5, "BrandA", "ModelX"); //

        // Link customer and comparison bidirectionally for tests if needed
        customer.getComparisons().add(comparison);
    }

    // --- Test addFirstProductToComparison ---

    @Test
    void addFirstProductToComparison_forCustomer_shouldCreateNewComparison() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        // Assume no existing unnamed comparison for this customer and category
        when(comparisonRepository.findByCustomerAndCategory(customer, category1)).thenReturn(Optional.empty());
        // Mock saving the new comparison
        when(comparisonRepository.save(any(ProductComparison.class))).thenAnswer(invocation -> {
            ProductComparison savedComp = invocation.getArgument(0);
            savedComp.setComparisonId(51L); // Assign an ID upon save
            return savedComp;
        });
        // Mock mapper
        when(mapper.productToDtoProduct(product1)).thenReturn(dtoProduct1);


        DtoComparison result = comparisonService.addFirstProductToComparison(101L, 1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().get(0).getProductId()).isEqualTo(101L);
        assertThat(result.getCategoryId()).isEqualTo(category1.getCategoryId());
        assertThat(result.getCategoryName()).isEqualTo(category1.getName());

        verify(productRepository).findById(101L);
        verify(customerRepository).findById(1L);
        verify(comparisonRepository).findByCustomerAndCategory(customer, category1);
        verify(comparisonRepository).save(any(ProductComparison.class));
        verify(mapper).productToDtoProduct(product1);
    }

     @Test
    void addFirstProductToComparison_forSession_shouldCreateNewComparison() {
        String sessionId = "test-session-123";
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        // Assume no existing comparison for this session and category
        when(comparisonRepository.findBySessionIdAndCategory(sessionId, category1)).thenReturn(Optional.empty());
        // Mock saving the new comparison
        when(comparisonRepository.save(any(ProductComparison.class))).thenAnswer(invocation -> {
            ProductComparison savedComp = invocation.getArgument(0);
            savedComp.setComparisonId(52L); // Assign an ID upon save
             savedComp.setSessionId(sessionId); // Ensure session ID is set
            return savedComp;
        });
        // Mock mapper
        when(mapper.productToDtoProduct(product1)).thenReturn(dtoProduct1);

        DtoComparison result = comparisonService.addFirstProductToComparison(101L, null, sessionId);

        assertThat(result).isNotNull();
        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().get(0).getProductId()).isEqualTo(101L);
        assertThat(result.getCategoryId()).isEqualTo(category1.getCategoryId());

        verify(productRepository).findById(101L);
        verify(comparisonRepository).findBySessionIdAndCategory(sessionId, category1);
        verify(comparisonRepository).save(any(ProductComparison.class));
        verify(mapper).productToDtoProduct(product1);
        verify(customerRepository, never()).findById(anyLong()); // Ensure customer repo not called for session
    }

    @Test
    void addFirstProductToComparison_productNotFound_shouldThrowException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.addFirstProductToComparison(999L, 1L, null);
        });
        // Checking the cause might be better if NotFoundException is public/accessible
        assertThat(ex.getMessage()).contains("Product not found");

        verify(productRepository).findById(999L);
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void addFirstProductToComparison_customerNotFound_shouldThrowException() {
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.addFirstProductToComparison(101L, 999L, null);
        });
        assertThat(ex.getMessage()).contains("Customer not found");

        verify(productRepository).findById(101L);
        verify(customerRepository).findById(999L);
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void addFirstProductToComparison_noCustomerIdOrSessionId_shouldThrowException() {
       when(productRepository.findById(101L)).thenReturn(Optional.of(product1));
       // Don't need customer repo mock here as it fails earlier

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            comparisonService.addFirstProductToComparison(101L, null, null);
        });
        assertThat(ex.getMessage()).contains("Cannot create comparison without customer ID or session ID");

        verify(productRepository).findById(101L); // Still checks product first
         verify(comparisonRepository, never()).save(any());
    }


    // --- Test addProductToExistingComparison ---

    @Test
    void addProductToExistingComparison_success() {
        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2));
        when(comparisonRepository.save(any(ProductComparison.class))).thenReturn(comparison); // Return the updated comparison
        // Mock mapper for both products now in the comparison
        when(mapper.productToDtoProduct(product1)).thenReturn(dtoProduct1);
        when(mapper.productToDtoProduct(product2)).thenReturn(new DtoProduct()); // Assume mapper returns a DTO for product2


        // Pre-condition: comparison has product1
        assertThat(comparison.getProducts()).containsExactly(product1);

        DtoComparison result = comparisonService.addProductToExistingComparison(50L, 102L);

        // Post-condition: comparison should now have product1 and product2
        assertThat(comparison.getProducts()).containsExactlyInAnyOrder(product1, product2);
        assertThat(result).isNotNull();
        assertThat(result.getComparisonId()).isEqualTo(50L);
         // Because the mapper is mocked simply, we check size. Real mapper mock would return 2 distinct DTOs.
        assertThat(result.getProducts()).hasSize(2);


        verify(comparisonRepository).findById(50L);
        verify(productRepository).findById(102L);
        verify(comparisonRepository).save(comparison);
        verify(mapper, times(2)).productToDtoProduct(any(Product.class)); // Called during conversion
    }

    @Test
    void addProductToExistingComparison_comparisonNotFound_shouldThrowException() {
        when(comparisonRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.addProductToExistingComparison(99L, 102L);
        });
        assertThat(ex.getMessage()).contains("Comparison not found");

        verify(comparisonRepository).findById(99L);
        verify(productRepository, never()).findById(anyLong());
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void addProductToExistingComparison_productNotFound_shouldThrowException() {
        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

         RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.addProductToExistingComparison(50L, 999L);
        });
         assertThat(ex.getMessage()).contains("Product not found");


        verify(comparisonRepository).findById(50L);
        verify(productRepository).findById(999L);
        verify(comparisonRepository, never()).save(any());
    }

    @Test
    void addProductToExistingComparison_categoryMismatch_shouldThrowException() {
        Product product3 = new Product();
        product3.setProductId(103L);
        product3.setName("Book Z");
        product3.setCategories(Set.of(category2)); // Different category!

        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison)); // Comparison is for category1
        when(productRepository.findById(103L)).thenReturn(Optional.of(product3));

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            comparisonService.addProductToExistingComparison(50L, 103L);
        });
        assertThat(ex.getMessage()).isEqualTo("Product category does not match comparison category.");

        verify(comparisonRepository).findById(50L);
        verify(productRepository).findById(103L);
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void addProductToExistingComparison_setsCategoryIfNotSet() {
        // Setup comparison without category initially
        ProductComparison emptyComparison = new ProductComparison();
         emptyComparison.setComparisonId(55L);
         emptyComparison.setCustomer(customer);
         emptyComparison.setProducts(new HashSet<>()); // Start empty
         emptyComparison.setCreatedAt(LocalDateTime.now());


        when(comparisonRepository.findById(55L)).thenReturn(Optional.of(emptyComparison));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1)); // product1 is in category1
         when(comparisonRepository.save(any(ProductComparison.class))).thenReturn(emptyComparison);
         when(mapper.productToDtoProduct(product1)).thenReturn(dtoProduct1);


        // Pre-condition: category is null
         assertThat(emptyComparison.getCategory()).isNull();

        DtoComparison result = comparisonService.addProductToExistingComparison(55L, 101L);

        // Post-condition: category should be set to product1's category
        assertThat(emptyComparison.getCategory()).isEqualTo(category1);
         assertThat(result).isNotNull();
         assertThat(result.getCategoryId()).isEqualTo(category1.getCategoryId());
         assertThat(result.getProducts()).hasSize(1);


        verify(comparisonRepository).findById(55L);
        verify(productRepository).findById(101L);
        verify(comparisonRepository).save(emptyComparison); // Ensure it was saved with the category set
         verify(mapper).productToDtoProduct(product1);
    }


    // --- Test removeProductFromComparison ---

    @Test
    void removeProductFromComparison_success() {
         // Start comparison with product1 and product2
         comparison.getProducts().add(product2);

        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison));
        when(productRepository.findById(101L)).thenReturn(Optional.of(product1)); // Product to remove
        when(comparisonRepository.save(any(ProductComparison.class))).thenReturn(comparison); // Return the updated comparison
        // Mock mapper only for the remaining product (product2)
        when(mapper.productToDtoProduct(product2)).thenReturn(new DtoProduct()); // Assume mapper maps product2

        // Pre-condition: has product1 and product2
        assertThat(comparison.getProducts()).containsExactlyInAnyOrder(product1, product2);

        DtoComparison result = comparisonService.removeProductFromComparison(50L, 101L);

        // Post-condition: should only have product2 left
         assertThat(comparison.getProducts()).doesNotContain(product1);
         assertThat(comparison.getProducts()).containsExactly(product2);
        assertThat(result).isNotNull();
        assertThat(result.getComparisonId()).isEqualTo(50L);
         assertThat(result.getProducts()).hasSize(1); // Only product2 DTO should be in the result

        verify(comparisonRepository).findById(50L);
        verify(productRepository).findById(101L);
        verify(comparisonRepository).save(comparison);
        verify(mapper).productToDtoProduct(product2); // Only called for product2 during conversion
         verify(mapper, never()).productToDtoProduct(product1); // Not called for removed product
    }

     @Test
    void removeProductFromComparison_productNotInComparison_shouldThrowException() {
        // comparison only has product1 by default in setUp
        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison));
        when(productRepository.findById(102L)).thenReturn(Optional.of(product2)); // Try to remove product2

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
             comparisonService.removeProductFromComparison(50L, 102L);
        });
         // Check for the specific cause if possible, otherwise message content
         assertThat(ex.getMessage()).contains("Product not found in this comparison.");
         assertThat(ex.getCause()).isInstanceOf(NotFoundException.class);


        verify(comparisonRepository).findById(50L);
        verify(productRepository).findById(102L);
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void removeProductFromComparison_comparisonNotFound_shouldThrowException() {
        when(comparisonRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.removeProductFromComparison(99L, 101L);
        });
        assertThat(ex.getMessage()).contains("Comparison not found");

        verify(comparisonRepository).findById(99L);
        verify(productRepository, never()).findById(anyLong());
        verify(comparisonRepository, never()).save(any());
    }

    // --- Test saveComparison ---

     @Test
    void saveComparison_success() {
         String comparisonName = "My Tech Compare";
        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(comparisonRepository.save(any(ProductComparison.class))).thenReturn(comparison);
        when(mapper.productToDtoProduct(product1)).thenReturn(dtoProduct1);

        // Assume comparison might have a session ID initially
        comparison.setSessionId("temp-session");

        DtoComparison result = comparisonService.saveComparison(50L, 1L, comparisonName);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(comparisonName);
         assertThat(comparison.getName()).isEqualTo(comparisonName); // Check entity was updated
         assertThat(comparison.getCustomer()).isEqualTo(customer);
         assertThat(comparison.getSessionId()).isNull(); // Session ID should be cleared
         assertThat(customer.getComparisons()).contains(comparison); // Bidirectional link check

        verify(comparisonRepository).findById(50L);
        verify(customerRepository).findById(1L);
        verify(comparisonRepository).save(comparison);
        verify(mapper).productToDtoProduct(product1);
    }

     @Test
    void saveComparison_comparisonNotFound_shouldThrowException() {
         when(comparisonRepository.findById(99L)).thenReturn(Optional.empty());

         RuntimeException ex = assertThrows(RuntimeException.class, () -> {
             comparisonService.saveComparison(99L, 1L, "New Name");
        });
         assertThat(ex.getMessage()).contains("Comparison not found");

        verify(comparisonRepository).findById(99L);
        verify(customerRepository, never()).findById(anyLong());
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void saveComparison_customerNotFound_shouldThrowException() {
        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison));
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

         RuntimeException ex = assertThrows(RuntimeException.class, () -> {
             comparisonService.saveComparison(50L, 999L, "New Name");
        });
         assertThat(ex.getMessage()).contains("Customer not found");


        verify(comparisonRepository).findById(50L);
        verify(customerRepository).findById(999L);
        verify(comparisonRepository, never()).save(any());
    }

     @Test
    void saveComparison_customerMismatch_shouldThrowException() {
         Customer otherCustomer = new Customer();
         otherCustomer.setUserId(2L);

        when(comparisonRepository.findById(50L)).thenReturn(Optional.of(comparison)); // Belongs to customer 1
        when(customerRepository.findById(2L)).thenReturn(Optional.of(otherCustomer)); // Trying to save for customer 2

        ValidationException ex = assertThrows(ValidationException.class, () -> {
            comparisonService.saveComparison(50L, 2L, "New Name");
        });
        assertThat(ex.getMessage()).isEqualTo("Comparison does not belong to this customer.");

        verify(comparisonRepository).findById(50L);
        verify(customerRepository).findById(2L);
        verify(comparisonRepository, never()).save(any());
    }


    // --- Test getDetailedComparison ---

    @Test
    void getDetailedComparison_success() {
        // Mock the specific method used for detailed fetching
        when(comparisonRepository.findByIdWithProducts(50L)).thenReturn(Optional.of(comparison));
        when(mapper.productToDtoProduct(product1)).thenReturn(dtoProduct1); // Assume only product1 initially

        DtoComparison result = comparisonService.getDetailedComparison(50L);

        assertThat(result).isNotNull();
        assertThat(result.getComparisonId()).isEqualTo(50L);
        assertThat(result.getProducts()).hasSize(1);
        assertThat(result.getProducts().get(0).getProductId()).isEqualTo(product1.getProductId());

        verify(comparisonRepository).findByIdWithProducts(50L);
        verify(mapper).productToDtoProduct(product1);
    }

     @Test
    void getDetailedComparison_notFound_shouldThrowException() {
        when(comparisonRepository.findByIdWithProducts(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.getDetailedComparison(99L);
        });
         assertThat(ex.getMessage()).contains("Comparison not found");
         assertThat(ex.getCause()).isInstanceOf(NotFoundException.class);


        verify(comparisonRepository).findByIdWithProducts(99L);
        verify(mapper, never()).productToDtoProduct(any());
    }

     // --- Test getSuperficialComparisons ---

    @Test
    void getSuperficialComparisons_forCustomer_success() {
         ProductComparison comparison2 = new ProductComparison(); // Another comparison for the same customer
         comparison2.setComparisonId(51L);
         comparison2.setCustomer(customer);
         comparison2.setName("Saved Compare");
         comparison2.setCategory(category1);
         comparison2.setProducts(new HashSet<>(Set.of(product2))); // Contains product2
         comparison2.setCreatedAt(LocalDateTime.now());


        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(comparisonRepository.findByCustomer(customer)).thenReturn(List.of(comparison, comparison2));

        // Mock the summary mapper call
        when(mapper.productToDtoProductSummary(product1)).thenReturn(dtoProductSummary1);
        when(mapper.productToDtoProductSummary(product2)).thenReturn(new DtoProductSummary()); // Mock summary for product2


        List<DtoComparison> results = comparisonService.getSuperficialComparisons(1L, null);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        // Note: The DTO structure is reused, but contains summaries.
        // The test verifies the correct mapper method was called.
        // We'll check the ID and that the summary mapper was called for the products in each.
         assertThat(results.get(0).getComparisonId()).isEqualTo(50L);
         assertThat(results.get(1).getComparisonId()).isEqualTo(51L);


        verify(customerRepository).findById(1L);
        verify(comparisonRepository).findByCustomer(customer);
        verify(mapper).productToDtoProductSummary(product1); // For comparison 1
        verify(mapper).productToDtoProductSummary(product2); // For comparison 2
         verify(mapper, never()).productToDtoProduct(any()); // Ensure detailed mapper not called
    }

     @Test
    void getSuperficialComparisons_forSession_success() {
        String sessionId = "session-abc";
        // comparison is associated with customer, let's create one for session
         ProductComparison sessionComparison = new ProductComparison();
         sessionComparison.setComparisonId(53L);
         sessionComparison.setSessionId(sessionId);
         sessionComparison.setCategory(category1);
         sessionComparison.setProducts(new HashSet<>(Set.of(product1)));
         sessionComparison.setCreatedAt(LocalDateTime.now());

        when(comparisonRepository.findBySessionId(sessionId)).thenReturn(List.of(sessionComparison));
        when(mapper.productToDtoProductSummary(product1)).thenReturn(dtoProductSummary1);

        List<DtoComparison> results = comparisonService.getSuperficialComparisons(null, sessionId);

        assertThat(results).isNotNull();
        assertThat(results).hasSize(1);
         assertThat(results.get(0).getComparisonId()).isEqualTo(53L);


        verify(customerRepository, never()).findById(anyLong());
        verify(comparisonRepository).findBySessionId(sessionId);
        verify(mapper).productToDtoProductSummary(product1);
        verify(mapper, never()).productToDtoProduct(any());
    }

     @Test
    void getSuperficialComparisons_noCustomerOrSession_returnsEmptyList() {
        List<DtoComparison> results = comparisonService.getSuperficialComparisons(null, null);
        assertThat(results).isNotNull();
        assertThat(results).isEmpty();
        verifyNoInteractions(customerRepository, comparisonRepository, mapper); // Nothing should be called
    }

     @Test
    void getSuperficialComparisons_customerNotFound_shouldThrowException() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            comparisonService.getSuperficialComparisons(999L, null);
        });
         assertThat(ex.getMessage()).contains("Customer not found");


        verify(customerRepository).findById(999L);
        verify(comparisonRepository, never()).findByCustomer(any());
        verify(mapper, never()).productToDtoProductSummary(any());
    }

}