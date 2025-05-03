package com.ecommerce.services.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entities.product.*;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.repository.*;
import com.ecommerce.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private ProductAttributeRepository productAttributeRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Seller testSeller;
    private Category testCategory;
    private Product testProduct;
    private DtoProduct testDtoProduct;
    private Long sellerId = 1L;
    private Long productId = 10L;
    private Long categoryId = 5L;


    @BeforeEach
    void setUp() {
        testSeller = new Seller();
        testSeller.setUserId(sellerId);
        testSeller.setUsername("Test Seller");

        testCategory = new Category();
        testCategory.setCategoryId(categoryId);
        testCategory.setName("Electronics");

        testProduct = new Product();
        testProduct.setProductId(productId);
        testProduct.setName("Test Product");
        testProduct.setDescription("A product for testing");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStockQuantity(100);
        testProduct.setSeller(testSeller);
        testProduct.setCategories(Collections.singleton(testCategory));
        testProduct.setApproved(false);

        testDtoProduct = new DtoProduct();
        testDtoProduct.setName("Test Product DTO");
        testDtoProduct.setDescription("DTO Description");
        testDtoProduct.setPrice(new BigDecimal("101.50"));
        testDtoProduct.setStockQuantity(50);
        testDtoProduct.setCategories(Collections.singleton(new DtoCategory(categoryId, "Electronics", null)));

        DtoProductImage dtoImage = new DtoProductImage();
        dtoImage.setImageUrl("http://example.com/image.jpg");
        dtoImage.setPrimary(true);
        dtoImage.setAltText("alt text");
        testDtoProduct.setImages(Collections.singletonList(dtoImage));

        DtoAttribute dtoAttribute = new DtoAttribute();
        dtoAttribute.setName("Color");
        dtoAttribute.setValue("Red");
        dtoAttribute.setAttributeGroup("Appearance");

        DtoVariant dtoVariant = new DtoVariant();
        dtoVariant.setSku("SKU123");
        dtoVariant.setPriceAdjustment(BigDecimal.ZERO);
        dtoVariant.setStockQuantity(10);
        dtoVariant.setAttributes(Collections.singletonList(dtoAttribute));
        testDtoProduct.setVariants(Collections.singletonList(dtoVariant));
    }

    @Test
    void createProduct_Success() {
        // Arrange
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(testSeller));
        when(categoryRepository.findAllById(Collections.singleton(categoryId)))
             .thenReturn(Collections.singletonList(testCategory));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product productToSave = invocation.getArgument(0);
            productToSave.setProductId(productId);
            return productToSave;
        });

        Product savedProductWithAssociations = createTestProductWithAssociations();
        when(productRepository.findById(productId)).thenReturn(Optional.of(savedProductWithAssociations));
        when(productImageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(productImageRepository.findByProductProductId(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductProductId(productId)).thenReturn(Collections.emptyList());

        // Act
        DtoProduct createdDto = productService.createProduct(testDtoProduct, sellerId);

        // Assert
        assertNotNull(createdDto);
        assertEquals(productId, createdDto.getProductId());
        assertEquals(testDtoProduct.getName(), createdDto.getName());
        assertNotNull(createdDto.getSeller());
        assertEquals(sellerId, createdDto.getSeller().getUserId());
        assertFalse(createdDto.getCategories().isEmpty());
        assertEquals(categoryId, createdDto.getCategories().iterator().next().getCategoryId());

        verify(sellerRepository, times(1)).findById(sellerId);
        verify(categoryRepository, times(1)).findAllById(Collections.singleton(categoryId));
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productImageRepository, times(1)).saveAll(anyList());
        verify(productVariantRepository, times(1)).saveAll(anyList());
        verify(productRepository, times(1)).findById(productId);
        verify(productImageRepository, times(1)).findByProductProductId(productId);
        verify(productVariantRepository, times(1)).findByProductProductId(productId);
    }

     private Product createTestProductWithAssociations() {
         Product p = new Product();
         p.setProductId(productId);
         p.setName(testDtoProduct.getName());
         p.setDescription(testDtoProduct.getDescription());
         p.setPrice(testDtoProduct.getPrice());
         p.setStockQuantity(testDtoProduct.getStockQuantity());
         p.setSeller(testSeller);
         p.setCategories(Collections.singleton(testCategory));
         p.setApproved(false);
         return p;
     }


    @Test
    void createProduct_SellerNotFound() {
        // Arrange
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            productService.createProduct(testDtoProduct, sellerId);
        }, "Should throw NoSuchElementException when seller is not found");

        verify(sellerRepository, times(1)).findById(sellerId);
        verifyNoInteractions(productRepository, categoryRepository, productImageRepository, productVariantRepository);
    }

    @Test
    void createProduct_CategoryNotFound() {
        // Arrange
        Long nonExistentCategoryId = 999L;
        testDtoProduct.setCategories(Collections.singleton(new DtoCategory(nonExistentCategoryId, "NonExistent", null)));

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(testSeller));
        when(categoryRepository.findAllById(Collections.singleton(nonExistentCategoryId)))
             .thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            productService.createProduct(testDtoProduct, sellerId);
        }, "Should throw NoSuchElementException when a category is not found");

        verify(sellerRepository, times(1)).findById(sellerId);
        verify(categoryRepository, times(1)).findAllById(Collections.singleton(nonExistentCategoryId));
        verifyNoInteractions(productRepository, productImageRepository, productVariantRepository);
    }


    @Test
    void getProductById_Success() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(productImageRepository.findByProductProductId(productId)).thenReturn(Collections.emptyList());
        when(productVariantRepository.findByProductProductId(productId)).thenReturn(Collections.emptyList());

        // Act
        DtoProduct foundDto = productService.getProductById(productId);

        // Assert
        assertNotNull(foundDto);
        assertEquals(productId, foundDto.getProductId());
        assertEquals(testProduct.getName(), foundDto.getName());
        assertNotNull(foundDto.getSeller());
        assertEquals(sellerId, foundDto.getSeller().getUserId());
        assertFalse(foundDto.getCategories().isEmpty());
        assertEquals(categoryId, foundDto.getCategories().iterator().next().getCategoryId());

        verify(productRepository, times(1)).findById(productId);
        verify(productImageRepository, times(1)).findByProductProductId(productId);
        verify(productVariantRepository, times(1)).findByProductProductId(productId);
        verifyNoMoreInteractions(sellerRepository, categoryRepository);
    }

    @Test
    void getProductById_NotFound() {
        // Arrange
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> {
            productService.getProductById(productId);
        }, "Should throw NoSuchElementException when product is not found");

        verify(productRepository, times(1)).findById(productId);
        verifyNoInteractions(sellerRepository, categoryRepository, productImageRepository, productVariantRepository, productAttributeRepository);
    }

    @Test
    void getAllProducts_ReturnsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> productList = Collections.singletonList(testProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, productList.size());

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(productImageRepository.findByProductProductIdAndIsPrimary(anyLong(), eq(true))).thenReturn(Optional.empty());
        when(productImageRepository.findFirstByProductProductIdOrderByImageIdAsc(anyLong())).thenReturn(Optional.empty());


        // Act
        Page<DtoProductSummary> resultPage = productService.getAllProducts(pageable);

        // Assert
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(1, resultPage.getContent().size());
        assertEquals(testProduct.getProductId(), resultPage.getContent().get(0).getProductId());
        assertEquals(testProduct.getName(), resultPage.getContent().get(0).getName());

        verify(productRepository, times(1)).findAll(pageable);
        verify(productImageRepository, times(1)).findByProductProductIdAndIsPrimary(productId, true);
        verify(productImageRepository, times(1)).findFirstByProductProductIdOrderByImageIdAsc(productId);
    }

     @Test
    void getProductsBySeller_Success() {
        // --- Arrange ---
        Pageable pageable = PageRequest.of(0, 5);
        List<Product> productList = Collections.singletonList(testProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, productList.size());

        when(sellerRepository.existsById(sellerId)).thenReturn(true);
        when(productRepository.findBySellerUserId(sellerId, pageable)).thenReturn(productPage);
        when(productImageRepository.findByProductProductIdAndIsPrimary(anyLong(), eq(true))).thenReturn(Optional.empty());
        when(productImageRepository.findFirstByProductProductIdOrderByImageIdAsc(anyLong())).thenReturn(Optional.empty());


        // --- Act ---
        Page<DtoProductSummary> resultPage = productService.getProductsBySeller(sellerId, pageable);

        // --- Assert ---
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertFalse(resultPage.getContent().isEmpty());
        assertEquals(testProduct.getProductId(), resultPage.getContent().get(0).getProductId());

        verify(sellerRepository, times(1)).existsById(sellerId);
        verify(productRepository, times(1)).findBySellerUserId(sellerId, pageable);
        verify(productImageRepository, times(1)).findByProductProductIdAndIsPrimary(productId, true);
        verify(productImageRepository, times(1)).findFirstByProductProductIdOrderByImageIdAsc(productId);
    }

    @Test
    void getProductsBySeller_SellerNotFound() {
        // --- Arrange ---
        Pageable pageable = PageRequest.of(0, 5);
        when(sellerRepository.existsById(sellerId)).thenReturn(false);

        // --- Act & Assert ---
        assertThrows(NoSuchElementException.class, () -> {
            productService.getProductsBySeller(sellerId, pageable);
        }, "Should throw NoSuchElementException when seller does not exist");

        verify(sellerRepository, times(1)).existsById(sellerId);
        verifyNoInteractions(productRepository);
    }

    @Test
    void getProductsByCategory_Success() {
        // --- Arrange ---
        Pageable pageable = PageRequest.of(0, 10);

        Product secondProduct = new Product();
        Long secondProductId = productId + 1;
        secondProduct.setProductId(secondProductId);
        secondProduct.setName("Second Test Product");
        secondProduct.setPrice(new BigDecimal("50.00"));
        secondProduct.setBrand("AnotherBrand");
        secondProduct.setModel("ModelB");
        secondProduct.setAverageRating(3.0);

        List<Product> productList = Arrays.asList(testProduct, secondProduct);
        Page<Product> productPage = new PageImpl<>(productList, pageable, productList.size());

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(productRepository.findByCategoriesContains(testCategory, pageable)).thenReturn(productPage);

        when(productImageRepository.findByProductProductIdAndIsPrimary(eq(productId), eq(true))).thenReturn(Optional.empty());
        when(productImageRepository.findFirstByProductProductIdOrderByImageIdAsc(eq(productId))).thenReturn(Optional.empty());
        when(productImageRepository.findByProductProductIdAndIsPrimary(eq(secondProductId), eq(true))).thenReturn(Optional.empty());
        when(productImageRepository.findFirstByProductProductIdOrderByImageIdAsc(eq(secondProductId))).thenReturn(Optional.empty());


        // --- Act ---
        Page<DtoProductSummary> resultPage = productService.getProductsByCategory(categoryId, pageable);

        // --- Assert ---
        assertNotNull(resultPage);
        assertEquals(productList.size(), resultPage.getTotalElements());
        assertEquals(productList.size(), resultPage.getContent().size());
        assertEquals(testProduct.getProductId(), resultPage.getContent().get(0).getProductId());
        assertEquals(secondProduct.getProductId(), resultPage.getContent().get(1).getProductId());

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(productRepository, times(1)).findByCategoriesContains(testCategory, pageable);
        verify(productImageRepository, times(productList.size())).findByProductProductIdAndIsPrimary(anyLong(), eq(true));
        verify(productImageRepository, times(productList.size())).findFirstByProductProductIdOrderByImageIdAsc(anyLong());
    }

    @Test
    void getProductsByCategory_CategoryNotFound() {
        // --- Arrange ---
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        assertThrows(NoSuchElementException.class, () -> {
            productService.getProductsByCategory(categoryId, pageable);
        }, "Should throw NoSuchElementException when category is not found");

        verify(categoryRepository, times(1)).findById(categoryId);
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateProduct_Success() {
        // --- Arrange ---
        DtoProduct updateDto = new DtoProduct();
        updateDto.setName("Updated Product Name");
        updateDto.setPrice(new BigDecimal("199.99"));
        updateDto.setStockQuantity(25);
        updateDto.setCategories(Collections.singleton(new DtoCategory(categoryId, "Electronics", null)));

        DtoProductImage newDtoImage = new DtoProductImage();
        newDtoImage.setImageUrl("http://example.com/new_image.jpg");
        newDtoImage.setPrimary(true);
        newDtoImage.setAltText("new alt");
        updateDto.setImages(Collections.singletonList(newDtoImage));

        DtoAttribute newDtoAttribute = new DtoAttribute();
        newDtoAttribute.setName("Size");
        newDtoAttribute.setValue("XL");
        newDtoAttribute.setAttributeGroup("Dimensions");

        DtoVariant newDtoVariant = new DtoVariant();
        newDtoVariant.setSku("SKU-UPD");
        newDtoVariant.setPriceAdjustment(BigDecimal.ONE);
        newDtoVariant.setStockQuantity(5);
        newDtoVariant.setAttributes(Collections.singletonList(newDtoAttribute));
        updateDto.setVariants(Collections.singletonList(newDtoVariant));


        when(categoryRepository.findAllById(Collections.singleton(categoryId)))
             .thenReturn(Collections.singletonList(testCategory));

        // Mock finding old variant for deletion cleanup
        ProductVariant oldVariant = new ProductVariant();
        oldVariant.setVariantId(50L);
        when(productVariantRepository.findByProductProductId(productId)).thenReturn(Collections.singletonList(oldVariant)); // First call

        // Mock deletions - Keep lenient stubbing for deleteByVariantId as it was previously needed
        lenient().doNothing().when(productAttributeRepository).deleteByVariantVariantId(oldVariant.getVariantId());
        doNothing().when(productVariantRepository).deleteByProductProductId(productId);
        doNothing().when(productImageRepository).deleteByProductProductId(productId);

        // Mock saves
        when(productImageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(productVariantRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Mock entities representing the state *after* save, for mapping
        Product updatedProductState = new Product();
        updatedProductState.setProductId(productId);
        updatedProductState.setName(updateDto.getName());
        updatedProductState.setPrice(updateDto.getPrice());
        updatedProductState.setStockQuantity(updateDto.getStockQuantity());
        updatedProductState.setSeller(testSeller);
        updatedProductState.setCategories(Collections.singleton(testCategory));

        ProductImage savedImage = new ProductImage();
        savedImage.setImageId(100L); savedImage.setProduct(updatedProductState); savedImage.setImageUrl("http://example.com/new_image.jpg"); savedImage.setPrimary(true); savedImage.setAltText("new alt");

        ProductAttribute savedAttribute = new ProductAttribute();
        savedAttribute.setAttributeId(200L); savedAttribute.setProduct(updatedProductState); savedAttribute.setName("Size"); savedAttribute.setValue("XL"); savedAttribute.setUnit(null); savedAttribute.setAttributeGroup("Dimensions");

        ProductVariant savedVariant = new ProductVariant();
        savedVariant.setVariantId(51L); savedVariant.setProduct(updatedProductState); savedVariant.setSku("SKU-UPD"); savedVariant.setPriceAdjustment(BigDecimal.ONE); savedVariant.setStockQuantity(5);
        savedVariant.setAttributes(Collections.singletonList(savedAttribute));
        savedAttribute.setVariant(savedVariant);

        // Chain the findById: first returns original, second returns updated state
        when(productRepository.findById(eq(productId)))
            .thenReturn(Optional.of(testProduct))          // For initial find
            .thenReturn(Optional.of(updatedProductState)); // For reload before mapping

        // Mocks for mapping calls *after* reload
        when(productImageRepository.findByProductProductId(productId)).thenReturn(Collections.singletonList(savedImage));
        // This second mock for findByProductProductId handles the call during mapping
        when(productVariantRepository.findByProductProductId(productId)).thenReturn(Collections.singletonList(oldVariant)) // For delete prep
                                                                       .thenReturn(Collections.singletonList(savedVariant)); // For mapping
        when(productAttributeRepository.findByVariantVariantId(savedVariant.getVariantId())).thenReturn(Collections.singletonList(savedAttribute));


        // --- Act ---
        DtoProduct resultDto = productService.updateProduct(productId, updateDto);

        // --- Assert ---
        assertNotNull(resultDto);
        assertEquals(updateDto.getName(), resultDto.getName());
        assertEquals(updateDto.getPrice(), resultDto.getPrice());
        assertEquals(updateDto.getStockQuantity(), resultDto.getStockQuantity());

        Product savedProduct = productCaptor.getValue();
        assertEquals(updateDto.getName(), savedProduct.getName());

        assertEquals(1, resultDto.getImages().size());
        assertEquals("http://example.com/new_image.jpg", resultDto.getImages().get(0).getImageUrl());
        assertEquals(1, resultDto.getVariants().size());
        assertEquals("SKU-UPD", resultDto.getVariants().get(0).getSku());
        assertEquals(1, resultDto.getVariants().get(0).getAttributes().size());
        assertEquals("Size", resultDto.getVariants().get(0).getAttributes().get(0).getName());

        // Verify interactions
        verify(productRepository, times(2)).findById(productId);
        verify(categoryRepository, times(1)).findAllById(anySet());
        verify(productImageRepository, times(1)).deleteByProductProductId(productId);
        verify(productVariantRepository, times(2)).findByProductProductId(productId); // Called twice

        // *** FIX: Use anyLong() for verification due to persistent mock issue ***
        verify(productAttributeRepository, times(1)).deleteByVariantVariantId(anyLong()); // Verify the call happened once for old variant attrs
        // *** END FIX ***

        verify(productVariantRepository, times(1)).deleteByProductProductId(productId);
        verify(productImageRepository, times(1)).saveAll(anyList());
        verify(productVariantRepository, times(1)).saveAll(anyList());
        verify(productRepository, times(1)).save(any(Product.class));

        verify(productImageRepository, times(1)).findByProductProductId(productId); // Mapping call
        verify(productAttributeRepository, times(1)).findByVariantVariantId(savedVariant.getVariantId()); // Mapping call for new attrs
    }


    @Test
    void updateProduct_NotFound() {
        // --- Arrange ---
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        assertThrows(NoSuchElementException.class, () -> {
            productService.updateProduct(productId, testDtoProduct);
        }, "Should throw NoSuchElementException when product to update is not found");

        verify(productRepository, times(1)).findById(productId);
        verifyNoMoreInteractions(productRepository);
        verifyNoInteractions(categoryRepository, productImageRepository, productVariantRepository, productAttributeRepository);
    }

    @Test
    void deleteProduct_Success() {
        // --- Arrange ---
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        ProductVariant variantToDelete = new ProductVariant();
        variantToDelete.setVariantId(99L);
        when(productVariantRepository.findByProductProductId(productId))
           .thenReturn(Collections.singletonList(variantToDelete));

        doNothing().when(productAttributeRepository).deleteByVariantVariantId(variantToDelete.getVariantId());
        doNothing().when(productVariantRepository).deleteByProductProductId(productId);
        doNothing().when(productImageRepository).deleteByProductProductId(productId);
        doNothing().when(productRepository).delete(testProduct);


        // --- Act ---
        productService.deleteProduct(productId);

        // --- Assert ---
        verify(productRepository, times(1)).findById(productId);
        verify(productVariantRepository, times(1)).findByProductProductId(productId);
        verify(productAttributeRepository, times(1)).deleteByVariantVariantId(variantToDelete.getVariantId());
        verify(productVariantRepository, times(1)).deleteByProductProductId(productId);
        verify(productImageRepository, times(1)).deleteByProductProductId(productId);
        verify(productRepository, times(1)).delete(testProduct);
    }

    @Test
    void deleteProduct_NotFound() {
        // --- Arrange ---
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        assertThrows(NoSuchElementException.class, () -> {
            productService.deleteProduct(productId);
        }, "Should throw NoSuchElementException when product to delete is not found");

        verify(productRepository, times(1)).findById(productId);
        verifyNoMoreInteractions(productRepository);
        verifyNoInteractions(productImageRepository, productVariantRepository, productAttributeRepository);
    }


    @Test
    void approveProduct_Success() {
        // --- Arrange ---
        testProduct.setApproved(false);
        testProduct.setApprovedAt(null);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(productCaptor.capture())).thenAnswer(inv -> {
            Product saved = inv.getArgument(0);
            Product resultProduct = new Product();
            resultProduct.setProductId(saved.getProductId());
            resultProduct.setName(saved.getName());
            resultProduct.setSeller(saved.getSeller());
            resultProduct.setCategories(saved.getCategories());
            resultProduct.setPrice(saved.getPrice());
            resultProduct.setApproved(saved.isApproved());
            resultProduct.setApprovedAt(saved.getApprovedAt());
            return resultProduct;
        });

        when(productRepository.findById(eq(productId)))
              .thenReturn(Optional.of(testProduct)) // Initial find
              .thenAnswer(invocation -> { // Second find (reload)
                  Product captured = productCaptor.getValue();
                  Product resultProduct = new Product();
                  resultProduct.setProductId(captured.getProductId());
                  resultProduct.setName(captured.getName());
                  resultProduct.setSeller(captured.getSeller());
                  resultProduct.setCategories(captured.getCategories());
                  resultProduct.setPrice(captured.getPrice());
                  resultProduct.setApproved(captured.isApproved());
                  resultProduct.setApprovedAt(captured.getApprovedAt());

                  when(productImageRepository.findByProductProductId(productId)).thenReturn(Collections.emptyList());
                  when(productVariantRepository.findByProductProductId(productId)).thenReturn(Collections.emptyList());

                  return Optional.of(resultProduct);
              });

        // --- Act ---
        DtoProduct approvedDto = productService.approveProduct(productId);

        // --- Assert ---
        assertNotNull(approvedDto);
        assertEquals(productId, approvedDto.getProductId());

        Product savedProduct = productCaptor.getValue();
        assertTrue(savedProduct.isApproved(), "Product should be marked as approved");
        assertNotNull(savedProduct.getApprovedAt(), "Approval timestamp should be set");

        verify(productRepository, times(2)).findById(productId); // Find + reload
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productImageRepository, times(1)).findByProductProductId(productId);
        verify(productVariantRepository, times(1)).findByProductProductId(productId);
    }

    @Test
    void approveProduct_NotFound() {
        // --- Arrange ---
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        assertThrows(NoSuchElementException.class, () -> {
            productService.approveProduct(productId);
        }, "Should throw NoSuchElementException when product to approve is not found");

        verify(productRepository, times(1)).findById(productId);
        verifyNoMoreInteractions(productRepository);
    }
}
