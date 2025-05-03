package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Enable Mockito annotations
class SellerProductServiceImplTest {

    @Mock // Create a mock instance of ProductService
    private ProductService baseProductService;

    @Mock // Create a mock instance of ProductRepository
    private ProductRepository productRepository;

    @InjectMocks // Inject the mocks into SellerProductServiceImpl
    private SellerProductServiceImpl sellerProductService;

    private DtoProduct testDtoProduct;
    private Product testProduct;
    private Seller testSeller;
    private Long sellerId = 1L;
    private Long otherSellerId = 2L;
    private Long productId = 10L;
    private Long nonExistentProductId = 99L;

    @BeforeEach
    void setUp() {
        // Setup common test data
        testSeller = new Seller();
        testSeller.setUserId(sellerId); // Use userId from User base class
        testSeller.setCompanyName("Test Seller Inc."); //

        testDtoProduct = new DtoProduct(); //
        testDtoProduct.setProductId(productId); //
        testDtoProduct.setName("Test Product"); //
        testDtoProduct.setPrice(new BigDecimal("99.99")); //
        // Add other necessary fields from DtoProduct if required for the tests

        testProduct = new Product(); //
        testProduct.setProductId(productId); //
        testProduct.setName("Test Product"); //
        testProduct.setSeller(testSeller); //
        testProduct.setPrice(new BigDecimal("99.99")); //
         // Add other necessary fields from Product if required for the tests

    }

    @Test
    @DisplayName("Create My Product - Success")
    void createMyProduct_shouldDelegateToBaseService() {
        // Arrange
        when(baseProductService.createProduct(any(DtoProduct.class), eq(sellerId))).thenReturn(testDtoProduct); // Mock base service call

        // Act
        DtoProduct createdProduct = sellerProductService.createMyProduct(testDtoProduct, sellerId); // Call the method under test

        // Assert
        assertNotNull(createdProduct);
        assertEquals(testDtoProduct.getName(), createdProduct.getName());
        verify(baseProductService, times(1)).createProduct(testDtoProduct, sellerId); // Verify base service was called once
    }

    @Test
    @DisplayName("Get My Products - Success")
    void getMyProducts_shouldDelegateToBaseService() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        DtoProductSummary summary = new DtoProductSummary(productId, "Test Product", new BigDecimal("99.99"), "url", 4.5, "Brand", "Model"); //
        Page<DtoProductSummary> expectedPage = new PageImpl<>(Collections.singletonList(summary)); //
        when(baseProductService.getProductsBySeller(eq(sellerId), any(Pageable.class))).thenReturn(expectedPage); // Mock base service call

        // Act
        Page<DtoProductSummary> actualPage = sellerProductService.getMyProducts(sellerId, pageable); // Call the method under test

        // Assert
        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        assertEquals(summary.getName(), actualPage.getContent().get(0).getName());
        verify(baseProductService, times(1)).getProductsBySeller(sellerId, pageable); // Verify base service was called
    }

    @Test
    @DisplayName("Update My Product - Success")
    void updateMyProduct_whenOwner_shouldUpdateAndReturnDto() {
        // Arrange: Mock ownership check and base service update
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct)); // Mock finding the product
        when(baseProductService.updateProduct(eq(productId), any(DtoProduct.class))).thenReturn(testDtoProduct); // Mock successful update

        // Act
        DtoProduct updatedDto = sellerProductService.updateMyProduct(productId, testDtoProduct, sellerId); // Call update

        // Assert
        assertNotNull(updatedDto);
        assertEquals(testDtoProduct.getProductId(), updatedDto.getProductId());
        verify(productRepository, times(1)).findById(productId); // Verify ownership check happened
        verify(baseProductService, times(1)).updateProduct(productId, testDtoProduct); // Verify base service update happened
    }

    @Test
    @DisplayName("Update My Product - Product Not Found")
    void updateMyProduct_whenProductNotFound_shouldThrowNoSuchElementException() {
        // Arrange: Mock product not found
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty()); //

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            sellerProductService.updateMyProduct(nonExistentProductId, testDtoProduct, sellerId); //
        }, "Product not found with id: " + nonExistentProductId); // Check exception message

        verify(productRepository, times(1)).findById(nonExistentProductId); // Verify repository was called
        verify(baseProductService, never()).updateProduct(any(), any()); // Verify base service was NOT called
    }

    @Test
    @DisplayName("Update My Product - Not Owner")
    void updateMyProduct_whenNotOwner_shouldThrowAccessDeniedException() {
        // Arrange: Mock finding product owned by someone else
        Seller actualOwner = new Seller();
        actualOwner.setUserId(otherSellerId); //
        testProduct.setSeller(actualOwner); //
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct)); //

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            sellerProductService.updateMyProduct(productId, testDtoProduct, sellerId); // Attempt update by wrong seller
        }, "Access Denied: Seller does not own product with id: " + productId); // Check exception message

        verify(productRepository, times(1)).findById(productId); // Verify repository was called
        verify(baseProductService, never()).updateProduct(any(), any()); // Verify base service was NOT called
    }

    @Test
    @DisplayName("Delete My Product - Success")
    void deleteMyProduct_whenOwner_shouldDelete() {
        // Arrange: Mock ownership check and base service delete
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct)); // Mock finding the product
        doNothing().when(baseProductService).deleteProduct(productId); // Mock successful deletion

        // Act
        assertDoesNotThrow(() -> {
            sellerProductService.deleteMyProduct(productId, sellerId); // Call delete
        });

        // Assert
        verify(productRepository, times(1)).findById(productId); // Verify ownership check happened
        verify(baseProductService, times(1)).deleteProduct(productId); // Verify base service delete happened
    }

    @Test
    @DisplayName("Delete My Product - Product Not Found")
    void deleteMyProduct_whenProductNotFound_shouldThrowNoSuchElementException() {
        // Arrange: Mock product not found
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty()); //

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            sellerProductService.deleteMyProduct(nonExistentProductId, sellerId); //
        }, "Product not found with id: " + nonExistentProductId); // Check exception message

        verify(productRepository, times(1)).findById(nonExistentProductId); // Verify repository was called
        verify(baseProductService, never()).deleteProduct(any()); // Verify base service was NOT called
    }

    @Test
    @DisplayName("Delete My Product - Not Owner")
    void deleteMyProduct_whenNotOwner_shouldThrowAccessDeniedException() {
        // Arrange: Mock finding product owned by someone else
        Seller actualOwner = new Seller();
        actualOwner.setUserId(otherSellerId); //
        testProduct.setSeller(actualOwner); //
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct)); //

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> {
            sellerProductService.deleteMyProduct(productId, sellerId); // Attempt delete by wrong seller
        }, "Access Denied: Seller does not own product with id: " + productId); // Check exception message

        verify(productRepository, times(1)).findById(productId); // Verify repository was called
        verify(baseProductService, never()).deleteProduct(any()); // Verify base service was NOT called
    }
}
