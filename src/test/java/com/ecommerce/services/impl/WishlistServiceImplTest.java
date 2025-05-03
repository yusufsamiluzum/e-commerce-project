package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoWishlist;
import com.ecommerce.dto.DtoWishlistItem;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.Wishlist;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.mappers.WishlistMapper;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WishlistServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistMapper wishlistMapper;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    // Declare fields for test data
    private Customer testCustomer;
    private Product testProduct1;
    private Product testProduct2;
    private Wishlist testWishlist;
    private DtoWishlist testDtoWishlist;
    private DtoProductSummary testDtoProductSummary1;
    private DtoWishlistItem testDtoWishlistItem1; // <<< Declare as field

    @BeforeEach
    void setUp() {
        // --- Entity Setup ---
        testCustomer = new Customer();
        testCustomer.setUserId(1L);
        testCustomer.setUsername("testuser");

        testProduct1 = new Product();
        testProduct1.setProductId(101L);
        testProduct1.setName("Test Product 1");
        testProduct1.setPrice(new BigDecimal("19.99"));

        testProduct2 = new Product();
        testProduct2.setProductId(102L);
        testProduct2.setName("Test Product 2");
        testProduct2.setPrice(new BigDecimal("25.50"));

        testWishlist = new Wishlist();
        testWishlist.setWishlistId(50L);
        testWishlist.setUser(testCustomer);
        testWishlist.setProducts(new HashSet<>(Collections.singletonList(testProduct1))); // Initialize with one product

        // --- DTO Setup ---
        testDtoProductSummary1 = new DtoProductSummary(
                testProduct1.getProductId(),
                testProduct1.getName(),
                testProduct1.getPrice(),
                "image_url_1",
                4.5,
                "BrandX",
                "ModelY"
        );

        // Initialize the field variable
        testDtoWishlistItem1 = new DtoWishlistItem(testDtoProductSummary1); // <<< Assign to field

        testDtoWishlist = new DtoWishlist(
                testWishlist.getWishlistId(),
                new ArrayList<>(Collections.singletonList(testDtoWishlistItem1)) // <<< Use the field
        );
    }

    // --- Test findOrCreateWishlistByCustomerId (implicitly tested via other methods) ---

    // --- Tests for getWishlistForCustomer ---

    @Test
    void getWishlistForCustomer_CustomerExists_WishlistExists_ReturnsDto() {
        // Arrange
        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
        when(wishlistMapper.toWishlistDto(testWishlist)).thenReturn(testDtoWishlist);

        // Act
        DtoWishlist result = wishlistService.getWishlistForCustomer(testCustomer.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(testDtoWishlist.getWishlistId(), result.getWishlistId());
        assertEquals(1, result.getItems().size());
        assertEquals(testProduct1.getProductId(), result.getItems().get(0).getProduct().getProductId());
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(wishlistMapper).toWishlistDto(testWishlist);
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void getWishlistForCustomer_CustomerExists_WishlistDoesNotExist_CreatesAndReturnsDto() {
        // Arrange
        Wishlist newWishlist = new Wishlist();
        newWishlist.setWishlistId(51L); // Simulate DB assigning ID
        newWishlist.setUser(testCustomer);
        newWishlist.setProducts(new HashSet<>());

        DtoWishlist emptyDtoWishlist = new DtoWishlist(newWishlist.getWishlistId(), new ArrayList<>());

        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.empty()); // No wishlist found
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(newWishlist); // Mock saving the new wishlist
        when(wishlistMapper.toWishlistDto(newWishlist)).thenReturn(emptyDtoWishlist);

        // Act
        DtoWishlist result = wishlistService.getWishlistForCustomer(testCustomer.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(newWishlist.getWishlistId(), result.getWishlistId());
        assertTrue(result.getItems().isEmpty());
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(wishlistRepository).save(any(Wishlist.class)); // Verify save was called
        verify(wishlistMapper).toWishlistDto(newWishlist);
    }

    @Test
    void getWishlistForCustomer_CustomerDoesNotExist_ThrowsResourceNotFound() {
        // Arrange
        Long nonExistentCustomerId = 99L;
        when(customerRepository.findById(nonExistentCustomerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.getWishlistForCustomer(nonExistentCustomerId);
        });

        assertEquals("Customer not found with id : '" + nonExistentCustomerId + "'", exception.getMessage());
        verify(customerRepository).findById(nonExistentCustomerId);
        verify(wishlistRepository, never()).findByUser(any());
        verify(wishlistRepository, never()).save(any());
        verify(wishlistMapper, never()).toWishlistDto(any());
    }

    // --- Tests for addProductToWishlist ---

    @Test
    void addProductToWishlist_Success() {
        // Arrange
        Set<Product> expectedProducts = new HashSet<>(Arrays.asList(testProduct1, testProduct2));
        Wishlist updatedWishlist = new Wishlist(testWishlist.getWishlistId(), testCustomer, expectedProducts);

        DtoProductSummary testDtoProductSummary2 = new DtoProductSummary(testProduct2.getProductId(), testProduct2.getName(), testProduct2.getPrice(), "url2", 4.0, "B", "M");
        DtoWishlistItem testDtoWishlistItem2 = new DtoWishlistItem(testDtoProductSummary2);
        // Use the field testDtoWishlistItem1 here
        DtoWishlist expectedDto = new DtoWishlist(testWishlist.getWishlistId(), Arrays.asList(testDtoWishlistItem1, testDtoWishlistItem2));

        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById(testProduct2.getProductId())).thenReturn(Optional.of(testProduct2));
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(updatedWishlist);
        when(wishlistMapper.toWishlistDto(any(Wishlist.class))).thenReturn(expectedDto);

        // Act
        DtoWishlist result = wishlistService.addProductToWishlist(testCustomer.getUserId(), testProduct2.getProductId());

        // Assert
        assertNotNull(result);
        assertEquals(expectedDto.getWishlistId(), result.getWishlistId());
        assertEquals(2, result.getItems().size());
        assertTrue(result.getItems().stream().anyMatch(item -> item.getProduct().getProductId().equals(testProduct2.getProductId())));
        assertTrue(testWishlist.getProducts().contains(testProduct2));
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(productRepository).findById(testProduct2.getProductId());
        verify(wishlistRepository).save(testWishlist);
        verify(wishlistMapper).toWishlistDto(testWishlist);
    }

    @Test
    void addProductToWishlist_ProductAlreadyExists_NoSave_ReturnsDto() {
       // Arrange
        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById(testProduct1.getProductId())).thenReturn(Optional.of(testProduct1));
        when(wishlistMapper.toWishlistDto(testWishlist)).thenReturn(testDtoWishlist);

        // Act
        DtoWishlist result = wishlistService.addProductToWishlist(testCustomer.getUserId(), testProduct1.getProductId());

        // Assert
        assertNotNull(result);
        assertEquals(testDtoWishlist.getWishlistId(), result.getWishlistId());
        assertEquals(1, result.getItems().size());
        assertEquals(testProduct1.getProductId(), result.getItems().get(0).getProduct().getProductId());
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(productRepository).findById(testProduct1.getProductId());
        verify(wishlistRepository, never()).save(any(Wishlist.class));
        verify(wishlistMapper).toWishlistDto(testWishlist);
    }


    @Test
    void addProductToWishlist_CustomerNotFound_ThrowsResourceNotFound() {
        // Arrange
        Long nonExistentCustomerId = 99L;
        Long productId = 101L;
        when(customerRepository.findById(nonExistentCustomerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.addProductToWishlist(nonExistentCustomerId, productId);
        });

        assertEquals("Customer not found with id : '" + nonExistentCustomerId + "'", exception.getMessage());
        verify(customerRepository).findById(nonExistentCustomerId);
        verify(wishlistRepository, never()).findByUser(any());
        verify(productRepository, never()).findById(any());
        verify(wishlistRepository, never()).save(any());
        verify(wishlistMapper, never()).toWishlistDto(any());
    }

     @Test
    void addProductToWishlist_ProductNotFound_ThrowsResourceNotFound() {
        // Arrange
         Long customerId = 1L;
         Long nonExistentProductId = 999L;
         when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
         when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
         when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

         // Act & Assert
         ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
             wishlistService.addProductToWishlist(customerId, nonExistentProductId);
         });

         assertEquals("Product not found with id : '" + nonExistentProductId + "'", exception.getMessage());
         verify(customerRepository).findById(customerId);
         verify(wishlistRepository).findByUser(testCustomer);
         verify(productRepository).findById(nonExistentProductId);
         verify(wishlistRepository, never()).save(any(Wishlist.class));
         verify(wishlistMapper, never()).toWishlistDto(any());
    }


    // --- Tests for removeProductFromWishlist ---

    @Test
    void removeProductFromWishlist_Success() {
        // Arrange
        assertTrue(testWishlist.getProducts().contains(testProduct1));
        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById(testProduct1.getProductId())).thenReturn(Optional.of(testProduct1));

        // Act
        wishlistService.removeProductFromWishlist(testCustomer.getUserId(), testProduct1.getProductId());

        // Assert
        assertFalse(testWishlist.getProducts().contains(testProduct1));
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(productRepository).findById(testProduct1.getProductId());
        verify(wishlistRepository).save(testWishlist);
    }

    @Test
    void removeProductFromWishlist_CustomerNotFound_ThrowsResourceNotFound() {
        // Arrange
        Long nonExistentCustomerId = 99L;
        Long productId = 101L;
        when(customerRepository.findById(nonExistentCustomerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.removeProductFromWishlist(nonExistentCustomerId, productId);
        });

        assertEquals("Customer not found with id : '" + nonExistentCustomerId + "'", exception.getMessage());
        verify(customerRepository).findById(nonExistentCustomerId);
        verify(wishlistRepository, never()).findByUser(any());
        verify(productRepository, never()).findById(any());
        verify(wishlistRepository, never()).save(any());
    }

     @Test
    void removeProductFromWishlist_WishlistNotFound_ThrowsResourceNotFound() {
         // Arrange
         when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
         when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.empty());

         // Act & Assert
         ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
             wishlistService.removeProductFromWishlist(testCustomer.getUserId(), testProduct1.getProductId());
         });

         assertEquals("Wishlist not found with customerId : '" + testCustomer.getUserId() + "'", exception.getMessage());
         verify(customerRepository).findById(testCustomer.getUserId());
         verify(wishlistRepository).findByUser(testCustomer);
         verify(productRepository, never()).findById(anyLong());
         verify(wishlistRepository, never()).save(any());
    }

    @Test
    void removeProductFromWishlist_ProductNotFoundInRepo_ThrowsResourceNotFound() {
        // Arrange
        Long nonExistentProductId = 999L;
        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.removeProductFromWishlist(testCustomer.getUserId(), nonExistentProductId);
        });

        assertEquals("Product not found with id : '" + nonExistentProductId + "'", exception.getMessage());
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(productRepository).findById(nonExistentProductId);
        verify(wishlistRepository, never()).save(any());
    }

    @Test
    void removeProductFromWishlist_ProductNotInWishlist_ThrowsResourceNotFound() {
        // Arrange
        assertFalse(testWishlist.getProducts().contains(testProduct2));
        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(wishlistRepository.findByUser(testCustomer)).thenReturn(Optional.of(testWishlist));
        when(productRepository.findById(testProduct2.getProductId())).thenReturn(Optional.of(testProduct2));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            wishlistService.removeProductFromWishlist(testCustomer.getUserId(), testProduct2.getProductId());
        });

        assertEquals("Product not found with id " + testProduct2.getProductId() + " in Wishlist : '" + testWishlist.getWishlistId() + "'", exception.getMessage());

        verify(customerRepository).findById(testCustomer.getUserId());
        verify(wishlistRepository).findByUser(testCustomer);
        verify(productRepository).findById(testProduct2.getProductId());
        verify(wishlistRepository, never()).save(any());
    }
}
