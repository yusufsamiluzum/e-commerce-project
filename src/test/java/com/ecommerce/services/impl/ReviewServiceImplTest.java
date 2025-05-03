package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoReview;
import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.entities.Review;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.mappers.ReviewMapper; // Assuming this mapper exists and works
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger; // Mock or verify logger interactions if needed

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    // If ProductService interaction is uncommented in the service, mock it too
    // @Mock
    // private ProductService productService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Customer testCustomer;
    private Product testProduct;
    private Review testReview;
    private DtoReview testDtoReview;
    private DtoUserSummary testDtoUserSummary;

    @BeforeEach
    void setUp() {
        // Initialize test data
        testCustomer = new Customer();
        testCustomer.setUserId(1L);
        testCustomer.setUsername("testuser");

        testProduct = new Product();
        testProduct.setProductId(101L);
        testProduct.setName("Test Product");

        testReview = new Review();
        testReview.setReviewId(1001L);
        testReview.setCustomer(testCustomer);
        testReview.setProduct(testProduct);
        testReview.setRating(4);
        testReview.setComment("Good product!");
        testReview.setCreatedAt(LocalDateTime.now());

        testDtoUserSummary = new DtoUserSummary(testCustomer.getUserId(), testCustomer.getUsername(), null, null); //

        testDtoReview = new DtoReview( //
                testReview.getReviewId(),
                testReview.getRating(),
                testReview.getComment(),
                testReview.getCreatedAt(),
                testDtoUserSummary,
                testProduct.getProductId()
        );
    }

    // --- createReview Tests ---

    @Test
    void createReview_Success() {
        // Arrange
        DtoReview inputDto = new DtoReview(null, 5, "Excellent!", null, null, testProduct.getProductId()); // ID and createdAt are generated
        Long customerId = testCustomer.getUserId();
        Long productId = testProduct.getProductId();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer)); //
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct)); //
        when(reviewRepository.findByProductProductIdAndCustomerUserId(productId, customerId)).thenReturn(Optional.empty()); // No existing review
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> { //
            Review savedReview = invocation.getArgument(0);
            savedReview.setReviewId(1002L); // Simulate saving and getting an ID
            savedReview.setCreatedAt(LocalDateTime.now()); // Simulate @CreationTimestamp
            return savedReview;
        });

        // Mock static mapper if necessary, or ensure it works standalone
        // For simplicity, we assume ReviewMapper.toDtoReview works correctly here.
        // If it has dependencies, mock those or use a real instance if simple.

        // Act
        DtoReview createdDto = reviewService.createReview(inputDto, customerId, productId); //

        // Assert
        assertNotNull(createdDto);
        assertEquals(inputDto.getRating(), createdDto.getRating());
        assertEquals(inputDto.getComment(), createdDto.getComment());
        assertNotNull(createdDto.getReviewId());
        assertNotNull(createdDto.getCreatedAt());
        assertEquals(testProduct.getProductId(), createdDto.getProductId());
        assertNotNull(createdDto.getCustomer());
        assertEquals(testCustomer.getUserId(), createdDto.getCustomer().getUserId());

        verify(customerRepository, times(1)).findById(customerId);
        verify(productRepository, times(1)).findById(productId);
        verify(reviewRepository, times(1)).findByProductProductIdAndCustomerUserId(productId, customerId);
        verify(reviewRepository, times(1)).save(any(Review.class));
        // Verify product rating update if uncommented in service:
        // verify(productService, times(1)).updateProductRating(productId);
    }

    @Test
    void createReview_CustomerNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        DtoReview inputDto = new DtoReview(null, 5, "Excellent!", null, null, testProduct.getProductId());
        Long nonExistentCustomerId = 999L;
        Long productId = testProduct.getProductId();

        when(customerRepository.findById(nonExistentCustomerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(inputDto, nonExistentCustomerId, productId); //
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(customerRepository, times(1)).findById(nonExistentCustomerId);
        verify(productRepository, never()).findById(anyLong());
        verify(reviewRepository, never()).save(any(Review.class));
    }

     @Test
    void createReview_ProductNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        DtoReview inputDto = new DtoReview(null, 5, "Excellent!", null, null, 9999L);
        Long customerId = testCustomer.getUserId();
        Long nonExistentProductId = 9999L;

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.createReview(inputDto, customerId, nonExistentProductId); //
        });

        assertTrue(exception.getMessage().contains("Product not found"));
        verify(customerRepository, times(1)).findById(customerId);
        verify(productRepository, times(1)).findById(nonExistentProductId);
        verify(reviewRepository, never()).save(any(Review.class));
    }

     @Test
    void createReview_DuplicateReview_ThrowsIllegalStateException() {
        // Arrange
        DtoReview inputDto = new DtoReview(null, 5, "Excellent!", null, null, testProduct.getProductId());
        Long customerId = testCustomer.getUserId();
        Long productId = testProduct.getProductId();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(testCustomer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByProductProductIdAndCustomerUserId(productId, customerId)).thenReturn(Optional.of(testReview)); // Existing review found

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewService.createReview(inputDto, customerId, productId); //
        });

        assertTrue(exception.getMessage().contains("already reviewed"));
        verify(customerRepository, times(1)).findById(customerId);
        verify(productRepository, times(1)).findById(productId);
        verify(reviewRepository, times(1)).findByProductProductIdAndCustomerUserId(productId, customerId);
        verify(reviewRepository, never()).save(any(Review.class));
    }


    // --- getReviewsByProductId Tests ---

    @Test
    void getReviewsByProductId_Success() {
        // Arrange
        Long productId = testProduct.getProductId();
        when(productRepository.existsById(productId)).thenReturn(true); // Assume product exists check
        when(reviewRepository.findByProductProductId(productId)).thenReturn(List.of(testReview));

        // Act
        List<DtoReview> reviews = reviewService.getReviewsByProductId(productId); //

        // Assert
        assertNotNull(reviews);
        assertEquals(1, reviews.size());
        DtoReview foundDto = reviews.get(0);
        assertEquals(testReview.getReviewId(), foundDto.getReviewId());
        assertEquals(testReview.getRating(), foundDto.getRating());
        assertEquals(testProduct.getProductId(), foundDto.getProductId());
        assertEquals(testCustomer.getUserId(), foundDto.getCustomer().getUserId());

        verify(productRepository, times(1)).existsById(productId);
        verify(reviewRepository, times(1)).findByProductProductId(productId);
    }

     @Test
    void getReviewsByProductId_ProductNotFound_ReturnsEmptyList() {
        // Arrange
        Long nonExistentProductId = 9999L;
        when(productRepository.existsById(nonExistentProductId)).thenReturn(false);
        // Assuming the service logic doesn't throw but returns empty list as per comment in service code
        when(reviewRepository.findByProductProductId(nonExistentProductId)).thenReturn(Collections.emptyList());


        // Act
        List<DtoReview> reviews = reviewService.getReviewsByProductId(nonExistentProductId); //

        // Assert
        assertNotNull(reviews);
        assertTrue(reviews.isEmpty());

        verify(productRepository, times(1)).existsById(nonExistentProductId);
         // If existsById is false, findByProductProductId might not be called depending on implementation choice
         // Based on the code provided, it IS called even if product doesn't exist.
        verify(reviewRepository, times(1)).findByProductProductId(nonExistentProductId);
    }


    // --- getReviewsByCustomerId Tests ---

    @Test
    void getReviewsByCustomerId_Success() {
        // Arrange
        Long customerId = testCustomer.getUserId();
        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(reviewRepository.findByCustomerUserId(customerId)).thenReturn(List.of(testReview));

        // Act
        List<DtoReview> reviews = reviewService.getReviewsByCustomerId(customerId); //

        // Assert
        assertNotNull(reviews);
        assertEquals(1, reviews.size());
        DtoReview foundDto = reviews.get(0);
        assertEquals(testReview.getReviewId(), foundDto.getReviewId());
        assertEquals(testCustomer.getUserId(), foundDto.getCustomer().getUserId());


        verify(customerRepository, times(1)).existsById(customerId);
        verify(reviewRepository, times(1)).findByCustomerUserId(customerId);
    }

     @Test
    void getReviewsByCustomerId_CustomerNotFound_ReturnsEmptyList() {
        // Arrange
        Long nonExistentCustomerId = 999L;
        when(customerRepository.existsById(nonExistentCustomerId)).thenReturn(false);
        // Assuming service returns empty list
        when(reviewRepository.findByCustomerUserId(nonExistentCustomerId)).thenReturn(Collections.emptyList());

        // Act
        List<DtoReview> reviews = reviewService.getReviewsByCustomerId(nonExistentCustomerId); //

        // Assert
        assertNotNull(reviews);
        assertTrue(reviews.isEmpty());

        verify(customerRepository, times(1)).existsById(nonExistentCustomerId);
        // Based on the code provided, it IS called even if customer doesn't exist.
        verify(reviewRepository, times(1)).findByCustomerUserId(nonExistentCustomerId);
    }

    // --- getReviewById Tests ---

    @Test
    void getReviewById_Success() {
        // Arrange
        Long reviewId = testReview.getReviewId();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

        // Act
        DtoReview foundDto = reviewService.getReviewById(reviewId); //

        // Assert
        assertNotNull(foundDto);
        assertEquals(testReview.getReviewId(), foundDto.getReviewId());
        assertEquals(testReview.getComment(), foundDto.getComment());
        assertEquals(testCustomer.getUserId(), foundDto.getCustomer().getUserId());

        verify(reviewRepository, times(1)).findById(reviewId);
    }

     @Test
    void getReviewById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long nonExistentReviewId = 9999L;
        when(reviewRepository.findById(nonExistentReviewId)).thenReturn(Optional.empty());

        // Act & Assert
         ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.getReviewById(nonExistentReviewId); //
        });

        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewRepository, times(1)).findById(nonExistentReviewId);
    }

    // --- updateReview Tests ---

    @Test
    void updateReview_Success() {
        // Arrange
        Long reviewId = testReview.getReviewId();
        Long customerId = testCustomer.getUserId(); // The owner
        DtoReview updateDto = new DtoReview(reviewId, 3, "It was okay.", null, null, null); // Update rating and comment

        // Make a mutable copy for the 'save' operation simulation
        Review reviewToUpdate = new Review();
        reviewToUpdate.setReviewId(testReview.getReviewId());
        reviewToUpdate.setCustomer(testReview.getCustomer());
        reviewToUpdate.setProduct(testReview.getProduct());
        reviewToUpdate.setRating(testReview.getRating());
        reviewToUpdate.setComment(testReview.getComment());
        reviewToUpdate.setCreatedAt(testReview.getCreatedAt());


        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(reviewToUpdate));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return updated review

        // Act
        DtoReview updatedDto = reviewService.updateReview(reviewId, updateDto, customerId); //

        // Assert
        assertNotNull(updatedDto);
        assertEquals(reviewId, updatedDto.getReviewId());
        assertEquals(updateDto.getRating(), updatedDto.getRating()); // Check updated rating
        assertEquals(updateDto.getComment(), updatedDto.getComment()); // Check updated comment
        assertEquals(testCustomer.getUserId(), updatedDto.getCustomer().getUserId());
        assertEquals(testProduct.getProductId(), updatedDto.getProductId());

        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, times(1)).save(reviewToUpdate); // Verify save was called on the updated entity
        // Verify product rating update if uncommented in service:
        // verify(productService, times(1)).updateProductRating(testProduct.getProductId());
    }

     @Test
    void updateReview_NoChanges_ReturnsExistingDataWithoutSave() {
        // Arrange
        Long reviewId = testReview.getReviewId();
        Long customerId = testCustomer.getUserId();
        // DTO matches the existing review exactly
        DtoReview noChangeDto = new DtoReview(
            testReview.getReviewId(),
            testReview.getRating(),
            testReview.getComment(),
            testReview.getCreatedAt(), // Note: DTO shouldn't usually carry this for updates
            testDtoUserSummary,
            testProduct.getProductId()
        );


        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview)); // Return the original review

        // Act
        DtoReview resultDto = reviewService.updateReview(reviewId, noChangeDto, customerId); //

        // Assert
        assertNotNull(resultDto);
        assertEquals(testReview.getReviewId(), resultDto.getReviewId());
        assertEquals(testReview.getRating(), resultDto.getRating());
        assertEquals(testReview.getComment(), resultDto.getComment());

        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, never()).save(any(Review.class)); // Ensure save was NOT called
         // Verify product rating update was NOT triggered
        // verify(productService, never()).updateProductRating(anyLong());
    }

     @Test
    void updateReview_ReviewNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long nonExistentReviewId = 9999L;
        Long customerId = testCustomer.getUserId();
        DtoReview updateDto = new DtoReview(nonExistentReviewId, 3, "It was okay.", null, null, null);

        when(reviewRepository.findById(nonExistentReviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.updateReview(nonExistentReviewId, updateDto, customerId); //
        });

        assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewRepository, times(1)).findById(nonExistentReviewId);
        verify(reviewRepository, never()).save(any(Review.class));
    }

     @Test
    void updateReview_UnauthorizedCustomer_ThrowsSecurityException() {
        // Arrange
        Long reviewId = testReview.getReviewId();
        Long unauthorizedCustomerId = 998L; // Different customer
         DtoReview updateDto = new DtoReview(reviewId, 3, "It was okay.", null, null, null);

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview)); // Review belongs to customer 1L

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            reviewService.updateReview(reviewId, updateDto, unauthorizedCustomerId); //
        });

        assertTrue(exception.getMessage().contains("not authorized to update"));
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // --- deleteReview Tests ---

    @Test
    void deleteReview_Success() {
        // Arrange
        Long reviewId = testReview.getReviewId();
        Long customerId = testCustomer.getUserId(); // The owner

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
        // No need to mock delete, just verify it's called

        // Act
        reviewService.deleteReview(reviewId, customerId); //

        // Assert
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, times(1)).delete(testReview); // Verify delete was called with the correct review object
         // Verify product rating update if uncommented in service:
        // verify(productService, times(1)).updateProductRating(testProduct.getProductId());
    }

    @Test
    void deleteReview_ReviewNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long nonExistentReviewId = 9999L;
        Long customerId = testCustomer.getUserId();

        when(reviewRepository.findById(nonExistentReviewId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewService.deleteReview(nonExistentReviewId, customerId); //
        });

         assertTrue(exception.getMessage().contains("Review not found"));
        verify(reviewRepository, times(1)).findById(nonExistentReviewId);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    void deleteReview_UnauthorizedCustomer_ThrowsSecurityException() {
        // Arrange
        Long reviewId = testReview.getReviewId();
        Long unauthorizedCustomerId = 998L; // Different customer

        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview)); // Review belongs to customer 1L

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            reviewService.deleteReview(reviewId, unauthorizedCustomerId); //
        });

        assertTrue(exception.getMessage().contains("not authorized to delete"));
        verify(reviewRepository, times(1)).findById(reviewId);
        verify(reviewRepository, never()).delete(any(Review.class));
    }
}