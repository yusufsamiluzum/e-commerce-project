// src/test/java/com/ecommerce/services/impl/CartServiceImplTest.java
package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoCart;
import com.ecommerce.dto.DtoCartItem;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.cart.Cart;
import com.ecommerce.entities.cart.CartItem;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.CartOperationException;
import com.ecommerce.exceptions.InsufficientStockException;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.mappers.CartMapper; // Assuming this mapper exists
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;

    private MockedStatic<CartMapper> mockedCartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private Customer customer;
    private Product product1;
    private Product product2;
    private Cart cart;
    private CartItem cartItem1;
    private CartItem cartItem2;
    private DtoCart dtoCart;

    @BeforeEach
    void setUp() {
        mockedCartMapper = Mockito.mockStatic(CartMapper.class);

        customer = new Customer();
        customer.setUserId(1L);
      

        product1 = new Product();
        product1.setProductId(101L);
        product1.setName("Test Product 1");
        product1.setPrice(BigDecimal.valueOf(10.00));
        product1.setStockQuantity(50);

        product2 = new Product();
        product2.setProductId(102L);
        product2.setName("Test Product 2");
        product2.setPrice(BigDecimal.valueOf(25.50));
        product2.setStockQuantity(20);

        cart = new Cart();
        cart.setCartId(1L);
        cart.setCustomer(customer);

        cartItem1 = new CartItem();
        cartItem1.setCartItemId(1L);
        cartItem1.setCart(cart);
        cartItem1.setProduct(product1);
        cartItem1.setQuantity(2);

        cartItem2 = new CartItem();
        cartItem2.setCartItemId(2L);
        cartItem2.setCart(cart);
        cartItem2.setProduct(product2);
        cartItem2.setQuantity(1);

        cart.setItems(new ArrayList<>(List.of(cartItem1, cartItem2)));

        // Use the *real* DtoProductSummary now.
        // Adjust instantiation based on your DtoProductSummary class.
        DtoProductSummary dtoProductSummary1 = new DtoProductSummary(/* provide necessary args for product1 */);
        DtoProductSummary dtoProductSummary2 = new DtoProductSummary(/* provide necessary args for product2 */);

        DtoCartItem dtoCartItem1 = new DtoCartItem(cartItem1.getCartItemId(), cartItem1.getQuantity(), dtoProductSummary1);
        DtoCartItem dtoCartItem2 = new DtoCartItem(cartItem2.getCartItemId(), cartItem2.getQuantity(), dtoProductSummary2);

        dtoCart = new DtoCart(cart.getCartId(), List.of(dtoCartItem1, dtoCartItem2), BigDecimal.valueOf(45.50)); // Example total

        // Default static mock behavior - can be overridden in specific tests
        mockedCartMapper.when(() -> CartMapper.toDtoCart(any(Cart.class)))
                        .thenReturn(dtoCart);

        // REMOVED: General mock for customerRepository.findById - will mock specifically in tests that need it.
        // when(customerRepository.findById(anyLong())).thenAnswer(invocation -> { ... });
    }

    @AfterEach
    void tearDown() {
        if (mockedCartMapper != null) {
            mockedCartMapper.close();
        }
    }


    // --- Test Get Cart ---

    @Test
    @DisplayName("Get Cart By Customer ID - Existing Cart")
    void getCartByCustomerId_ExistingCart_ReturnsDtoCart() {
        // Arrange
        when(cartRepository.findByCustomerUserId(customer.getUserId())).thenReturn(Optional.of(cart));
        when(cartRepository.findByCustomerIdWithItems(customer.getUserId())).thenReturn(Optional.of(cart));

        // Act
        DtoCart result = cartService.getCartByCustomerId(customer.getUserId());

        // Assert
        assertNotNull(result);
        assertEquals(cart.getCartId(), result.getCartId());
        assertEquals(2, result.getItems().size());
        assertEquals(dtoCart.getCalculatedTotal(), result.getCalculatedTotal());
        verify(cartRepository, times(1)).findByCustomerUserId(customer.getUserId());
        verify(cartRepository, times(1)).findByCustomerIdWithItems(customer.getUserId());
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(cart));
        // No verify for customerRepository.findById needed here as it shouldn't be called
    }

    @Test
    @DisplayName("Get Cart By Customer ID - No Cart, Creates New")
    void getCartByCustomerId_NoCart_CreatesAndReturnsEmptyDtoCart() {
        // Arrange
        Long newCustomerId = 2L;
        Customer newCustomer = new Customer();
        newCustomer.setUserId(newCustomerId);
       

        Cart savedCart = new Cart();
        savedCart.setCartId(2L);
        savedCart.setCustomer(newCustomer);
        savedCart.setItems(new ArrayList<>());

        DtoCart emptyDtoCart = new DtoCart(savedCart.getCartId(), new ArrayList<>(), BigDecimal.ZERO);

        when(cartRepository.findByCustomerUserId(newCustomerId)).thenReturn(Optional.empty());
        // *Specific mock for customer lookup needed in this test*
        when(customerRepository.findById(newCustomerId)).thenReturn(Optional.of(newCustomer));
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartRepository.findByCustomerIdWithItems(newCustomerId)).thenReturn(Optional.of(savedCart));
        mockedCartMapper.when(() -> CartMapper.toDtoCart(savedCart)).thenReturn(emptyDtoCart);

        // Act
        DtoCart result = cartService.getCartByCustomerId(newCustomerId);

        // Assert
        assertNotNull(result);
        assertEquals(savedCart.getCartId(), result.getCartId());
        assertTrue(result.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getCalculatedTotal());
        verify(cartRepository, times(2)).findByCustomerUserId(newCustomerId);
        // Verify customer lookup was called
        verify(customerRepository, times(1)).findById(newCustomerId);
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartRepository, times(1)).findByCustomerIdWithItems(newCustomerId);
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(savedCart));
    }

     @Test
    @DisplayName("Get Cart By Customer ID - Customer Not Found During Creation")
    void getCartByCustomerId_CustomerNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long nonExistentCustomerId = 99L;
        when(cartRepository.findByCustomerUserId(nonExistentCustomerId)).thenReturn(Optional.empty());
        // *Specific mock for customer lookup needed in this test*
        when(customerRepository.findById(nonExistentCustomerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartService.getCartByCustomerId(nonExistentCustomerId);
        });
        assertEquals("Customer not found with ID: " + nonExistentCustomerId, exception.getMessage());
        verify(cartRepository, times(1)).findByCustomerUserId(nonExistentCustomerId);
        // Verify customer lookup was called
        verify(customerRepository, times(1)).findById(nonExistentCustomerId);
        verify(cartRepository, never()).save(any(Cart.class));
        verify(cartRepository, never()).findByCustomerIdWithItems(anyLong());
    }


    // --- Test Add Item ---
    // (No changes needed in the remaining tests regarding customerRepository.findById mocking
    // as they either don't hit that path or the exception happens earlier)

    @Test
    @DisplayName("Add Item To Cart - New Item")
    void addItemToCart_NewItem_AddsSuccessfully() {
        // Arrange
        Long customerId = customer.getUserId();
        Long productIdToAdd = product2.getProductId();
        int quantity = 1;

        Cart cartBeforeAdd = new Cart();
        cartBeforeAdd.setCartId(cart.getCartId());
        cartBeforeAdd.setCustomer(customer);
        cartBeforeAdd.setItems(new ArrayList<>(List.of(cartItem1)));

        CartItem newItem = new CartItem();
        newItem.setCart(cartBeforeAdd);
        newItem.setProduct(product2);
        newItem.setQuantity(quantity);
        newItem.setCartItemId(3L); // Assume generated ID

        Cart cartAfterAddingItem = new Cart();
        cartAfterAddingItem.setCartId(cart.getCartId());
        cartAfterAddingItem.setCustomer(customer);
        cartAfterAddingItem.setItems(new ArrayList<>(List.of(cartItem1, newItem)));

        DtoProductSummary summary1 = new DtoProductSummary(/* product1 details */);
        DtoProductSummary summary2 = new DtoProductSummary(/* product2 details */);
        DtoCartItem dtoItem1 = new DtoCartItem(cartItem1.getCartItemId(), cartItem1.getQuantity(), summary1);
        DtoCartItem dtoItem2 = new DtoCartItem(newItem.getCartItemId(), newItem.getQuantity(), summary2);
        DtoCart dtoAfterAdd = new DtoCart(cartAfterAddingItem.getCartId(), List.of(dtoItem1, dtoItem2), BigDecimal.valueOf(45.50)); // Adjust total
        mockedCartMapper.when(() -> CartMapper.toDtoCart(cartAfterAddingItem)).thenReturn(dtoAfterAdd);


        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cartBeforeAdd));
        when(productRepository.findById(productIdToAdd)).thenReturn(Optional.of(product2));
        when(cartItemRepository.findByCartCartIdAndProductProductId(cartBeforeAdd.getCartId(), productIdToAdd)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(newItem);
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cartAfterAddingItem));


        // Act
        DtoCart result = cartService.addItemToCart(customerId, productIdToAdd, quantity);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getItems().size());

        verify(cartRepository, times(2)).findByCustomerUserId(customerId);
        verify(productRepository, times(1)).findById(productIdToAdd);
        verify(cartItemRepository, times(1)).findByCartCartIdAndProductProductId(cartBeforeAdd.getCartId(), productIdToAdd);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(cartRepository, times(1)).findByCustomerIdWithItems(customerId);
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(cartAfterAddingItem));
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Add Item To Cart - Existing Item")
    void addItemToCart_ExistingItem_UpdatesQuantity() {
        // Arrange
        Long customerId = customer.getUserId();
        Long productId = cartItem1.getProduct().getProductId();
        int initialQuantity = cartItem1.getQuantity();
        int quantityToAdd = 3;
        int expectedNewQuantity = initialQuantity + quantityToAdd;

        Cart cartBeforeUpdate = cart;

        Cart cartAfterUpdate = new Cart();
        cartAfterUpdate.setCartId(cart.getCartId());
        cartAfterUpdate.setCustomer(customer);
        CartItem updatedItem1 = new CartItem();
        updatedItem1.setCartItemId(cartItem1.getCartItemId());
        updatedItem1.setCart(cartAfterUpdate);
        updatedItem1.setProduct(product1);
        updatedItem1.setQuantity(expectedNewQuantity);
        cartAfterUpdate.setItems(new ArrayList<>(List.of(updatedItem1, cartItem2)));

        DtoProductSummary summary1 = new DtoProductSummary(/* product1 details */);
        DtoProductSummary summary2 = new DtoProductSummary(/* product2 details */);
        DtoCartItem dtoItem1 = new DtoCartItem(updatedItem1.getCartItemId(), updatedItem1.getQuantity(), summary1);
        DtoCartItem dtoItem2 = new DtoCartItem(cartItem2.getCartItemId(), cartItem2.getQuantity(), summary2);
        DtoCart dtoAfterUpdate = new DtoCart(cartAfterUpdate.getCartId(), List.of(dtoItem1, dtoItem2), BigDecimal.valueOf(75.50)); // Adjust total
        mockedCartMapper.when(() -> CartMapper.toDtoCart(cartAfterUpdate)).thenReturn(dtoAfterUpdate);


        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cartBeforeUpdate));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByCartCartIdAndProductProductId(cartBeforeUpdate.getCartId(), productId)).thenReturn(Optional.of(cartItem1));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem1);
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cartAfterUpdate));


        // Act
        DtoCart result = cartService.addItemToCart(customerId, productId, quantityToAdd);

        // Assert
        assertNotNull(result);
        DtoCartItem resultItem1 = result.getItems().stream().filter(i -> i.getCartItemId().equals(updatedItem1.getCartItemId())).findFirst().orElse(null);
        assertNotNull(resultItem1);
        assertEquals(expectedNewQuantity, resultItem1.getQuantity());

        verify(cartRepository, times(2)).findByCustomerUserId(customerId);
        verify(productRepository, times(1)).findById(productId);
        verify(cartItemRepository, times(1)).findByCartCartIdAndProductProductId(cartBeforeUpdate.getCartId(), productId);
        verify(cartItemRepository, times(1)).save(cartItem1);
        assertEquals(expectedNewQuantity, cartItem1.getQuantity());
        verify(cartRepository, times(1)).findByCustomerIdWithItems(customerId);
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(cartAfterUpdate));
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Add Item To Cart - Insufficient Stock for New Item")
    void addItemToCart_InsufficientStock_NewItem_ThrowsInsufficientStockException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long productId = product1.getProductId();
        int quantity = 60;
        product1.setStockQuantity(50);

        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> {
            cartService.addItemToCart(customerId, productId, quantity);
        });
        assertEquals("Insufficient stock for product: " + product1.getName() + ". Available: " + product1.getStockQuantity(), exception.getMessage());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

     @Test
    @DisplayName("Add Item To Cart - Insufficient Stock for Existing Item Update")
    void addItemToCart_InsufficientStock_ExistingItem_ThrowsInsufficientStockException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long productId = cartItem1.getProduct().getProductId();
        cartItem1.setQuantity(48);
        int quantityToAdd = 5;
        int requiredTotal = cartItem1.getQuantity() + quantityToAdd;
        product1.setStockQuantity(50);

        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product1));
        when(cartItemRepository.findByCartCartIdAndProductProductId(cart.getCartId(), productId)).thenReturn(Optional.of(cartItem1));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> {
            cartService.addItemToCart(customerId, productId, quantityToAdd);
        });
        assertEquals("Insufficient stock for product: " + product1.getName() + ". Requested total: " + requiredTotal + ", Available: " + product1.getStockQuantity(), exception.getMessage());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Add Item To Cart - Product Not Found")
    void addItemToCart_ProductNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long nonExistentProductId = 999L;
        int quantity = 1;

        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartService.addItemToCart(customerId, nonExistentProductId, quantity);
        });
        assertEquals("Product not found with ID: " + nonExistentProductId, exception.getMessage());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

     @Test
    @DisplayName("Add Item To Cart - Negative Quantity")
    void addItemToCart_NegativeQuantity_ThrowsCartOperationException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long productId = product1.getProductId();
        int quantity = -1;

        // Act & Assert
        CartOperationException exception = assertThrows(CartOperationException.class, () -> {
            cartService.addItemToCart(customerId, productId, quantity);
        });
        assertEquals("Quantity must be positive.", exception.getMessage());
        verify(cartRepository, never()).findByCustomerUserId(any());
        verify(productRepository, never()).findById(any());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }


    // --- Test Update Item Quantity ---

    @Test
    @DisplayName("Update Cart Item Quantity - Success")
    void updateCartItemQuantity_Success() {
        // Arrange
        Long customerId = customer.getUserId();
        Long cartItemId = cartItem1.getCartItemId();
        int newQuantity = 5;
        product1.setStockQuantity(10);

        Cart cartAfterUpdate = new Cart();
        cartAfterUpdate.setCartId(cart.getCartId());
        cartAfterUpdate.setCustomer(customer);
        CartItem updatedItem1 = new CartItem();
        updatedItem1.setCartItemId(cartItem1.getCartItemId());
        updatedItem1.setCart(cartAfterUpdate);
        updatedItem1.setProduct(product1);
        updatedItem1.setQuantity(newQuantity);
        cartAfterUpdate.setItems(new ArrayList<>(List.of(updatedItem1, cartItem2)));

        DtoProductSummary summary1 = new DtoProductSummary(/* product1 details */);
        DtoProductSummary summary2 = new DtoProductSummary(/* product2 details */);
        DtoCartItem dtoItem1 = new DtoCartItem(updatedItem1.getCartItemId(), updatedItem1.getQuantity(), summary1);
        DtoCartItem dtoItem2 = new DtoCartItem(cartItem2.getCartItemId(), cartItem2.getQuantity(), summary2);
        DtoCart dtoAfterUpdate = new DtoCart(cartAfterUpdate.getCartId(), List.of(dtoItem1, dtoItem2), BigDecimal.valueOf(75.50)); // Adjust total
        mockedCartMapper.when(() -> CartMapper.toDtoCart(cartAfterUpdate)).thenReturn(dtoAfterUpdate);


        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), cartItemId)).thenReturn(Optional.of(cartItem1));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem1);
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cartAfterUpdate));


        // Act
        DtoCart result = cartService.updateCartItemQuantity(customerId, cartItemId, newQuantity);

        // Assert
        assertNotNull(result);
        DtoCartItem resultItem1 = result.getItems().stream().filter(i -> i.getCartItemId().equals(updatedItem1.getCartItemId())).findFirst().orElse(null);
        assertNotNull(resultItem1);
        assertEquals(newQuantity, resultItem1.getQuantity());

        verify(cartRepository, times(2)).findByCustomerUserId(customerId);
        verify(cartItemRepository, times(1)).findByCartCartIdAndCartItemId(cart.getCartId(), cartItemId);
        verify(cartItemRepository, times(1)).save(cartItem1);
        assertEquals(newQuantity, cartItem1.getQuantity());
        verify(cartRepository, times(1)).findByCustomerIdWithItems(customerId);
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(cartAfterUpdate));
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Update Cart Item Quantity - Insufficient Stock")
    void updateCartItemQuantity_InsufficientStock_ThrowsInsufficientStockException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long cartItemId = cartItem1.getCartItemId();
        int newQuantity = 15;
        product1.setStockQuantity(10);

        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), cartItemId)).thenReturn(Optional.of(cartItem1));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> {
            cartService.updateCartItemQuantity(customerId, cartItemId, newQuantity);
        });
         assertEquals("Insufficient stock for product: " + product1.getName() + ". Requested: " + newQuantity + ", Available: " + product1.getStockQuantity(), exception.getMessage());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Update Cart Item Quantity - Item Not Found")
    void updateCartItemQuantity_ItemNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long nonExistentCartItemId = 999L;
        int newQuantity = 5;

        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), nonExistentCartItemId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartService.updateCartItemQuantity(customerId, nonExistentCartItemId, newQuantity);
        });
        assertEquals("Cart item not found with ID: " + nonExistentCartItemId + " in cart for customer " + customerId, exception.getMessage());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Update Cart Item Quantity - Zero Quantity")
    void updateCartItemQuantity_ZeroQuantity_ThrowsCartOperationException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long cartItemId = cartItem1.getCartItemId();
        int quantity = 0;

        // Act & Assert
        CartOperationException exception = assertThrows(CartOperationException.class, () -> {
            cartService.updateCartItemQuantity(customerId, cartItemId, quantity);
        });
        assertEquals("Quantity must be positive.", exception.getMessage());
        verify(cartRepository, never()).findByCustomerUserId(any());
        verify(cartItemRepository, never()).findByCartCartIdAndCartItemId(any(), any());
        verify(cartItemRepository, never()).save(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }


    // --- Test Remove Item ---

    @Test
    @DisplayName("Remove Item From Cart - Success")
    void removeItemFromCart_Success() {
        // Arrange
        Long customerId = customer.getUserId();
        Long cartItemIdToRemove = cartItem1.getCartItemId();
        CartItem itemToRemove = cartItem1;

        Cart cartAfterRemove = new Cart();
        cartAfterRemove.setCartId(cart.getCartId());
        cartAfterRemove.setCustomer(customer);
        cartAfterRemove.setItems(new ArrayList<>(List.of(cartItem2)));

        DtoProductSummary summary2 = new DtoProductSummary(/* product2 details */);
        DtoCartItem dtoItem2 = new DtoCartItem(cartItem2.getCartItemId(), cartItem2.getQuantity(), summary2);
        DtoCart dtoAfterRemove = new DtoCart(cartAfterRemove.getCartId(), List.of(dtoItem2), BigDecimal.valueOf(25.50)); // Adjust total
        mockedCartMapper.when(() -> CartMapper.toDtoCart(cartAfterRemove)).thenReturn(dtoAfterRemove);


        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), cartItemIdToRemove)).thenReturn(Optional.of(itemToRemove));
        doNothing().when(cartItemRepository).delete(itemToRemove);
        when(cartRepository.findByCustomerIdWithItems(customerId)).thenReturn(Optional.of(cartAfterRemove));


        // Act
        DtoCart result = cartService.removeItemFromCart(customerId, cartItemIdToRemove);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(cartItem2.getCartItemId(), result.getItems().get(0).getCartItemId());

        verify(cartRepository, times(2)).findByCustomerUserId(customerId);
        verify(cartItemRepository, times(1)).findByCartCartIdAndCartItemId(cart.getCartId(), cartItemIdToRemove);
        verify(cartItemRepository, times(1)).delete(itemToRemove);
        verify(cartRepository, times(1)).findByCustomerIdWithItems(customerId);
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(cartAfterRemove));
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

     @Test
    @DisplayName("Remove Item From Cart - Item Not Found")
    void removeItemFromCart_ItemNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long customerId = customer.getUserId();
        Long nonExistentCartItemId = 999L;

        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), nonExistentCartItemId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartService.removeItemFromCart(customerId, nonExistentCartItemId);
        });
        assertEquals("Cart item not found with ID: " + nonExistentCartItemId + " in cart for customer " + customerId, exception.getMessage());
        verify(cartItemRepository, never()).delete(any());
        verify(cartRepository, never()).findByCustomerIdWithItems(customerId);
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    // --- Test Clear Cart ---

    @Test
    @DisplayName("Clear Cart - Success")
    void clearCart_Success_DeletesAllItems() {
        // Arrange
        Long customerId = customer.getUserId();
        List<CartItem> itemsInCartBeforeClear = List.copyOf(cart.getItems());

        Cart emptyCartState = new Cart();
        emptyCartState.setCartId(cart.getCartId());
        emptyCartState.setCustomer(cart.getCustomer());
        emptyCartState.setItems(new ArrayList<>());

        DtoCart emptyDtoCart = new DtoCart(emptyCartState.getCartId(), new ArrayList<>(), BigDecimal.ZERO);
        mockedCartMapper.when(() -> CartMapper.toDtoCart(cart)).thenReturn(emptyDtoCart);


        when(cartRepository.findByCustomerUserId(customerId)).thenReturn(Optional.of(cart));
        doNothing().when(cartItemRepository).deleteAll(anyList());


        // Act
        DtoCart result = cartService.clearCart(customerId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getCalculatedTotal());

        verify(cartRepository, times(1)).findByCustomerUserId(customerId);
        verify(cartItemRepository, times(1)).deleteAll(itemsInCartBeforeClear);
        mockedCartMapper.verify(() -> CartMapper.toDtoCart(cart));
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }

    @Test
    @DisplayName("Clear Cart - Cart Not Found")
    void clearCart_CartNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        Long nonExistentCustomerId = 99L;
        when(cartRepository.findByCustomerUserId(nonExistentCustomerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            cartService.clearCart(nonExistentCustomerId);
        });
        assertEquals("Cart not found for customer ID: " + nonExistentCustomerId, exception.getMessage());
        verify(cartItemRepository, never()).deleteAll(anyList());
        verify(cartRepository, never()).findByCustomerIdWithItems(anyLong());
        verify(customerRepository, never()).findById(any()); // Should not be called here
    }
}

