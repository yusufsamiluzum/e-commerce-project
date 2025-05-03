package com.ecommerce.services.impl;

import com.ecommerce.dto.*;
import com.ecommerce.entities.Payment;
import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.order.OrderItem;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.*;
import com.ecommerce.mappers.OrderMapper;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList; // Import ArrayList

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private OrderMapper orderMapper; // Assuming OrderMapper is correctly configured

    // Mocks for mappers used by OrderMapper (if needed, depends on OrderMapper implementation)
    // @Mock private OrderItemMapper orderItemMapper;
    // @Mock private ProductMapper productMapper;
    // @Mock private AddressMapper addressMapper;
    // @Mock private PaymentMapper paymentMapper;
    // @Mock private UserMapper userMapper;


    @InjectMocks
    private OrderServiceImpl orderService;

    private Customer testCustomer;
    private Address testShippingAddress;
    private Address testBillingAddress;
    private Product testProduct1;
    private Product testProduct2;
    private Order testOrder;
    private DtoOrderRequest testOrderRequest;
    private DtoOrderResponse expectedOrderResponse; // Renamed for clarity
    private Payment testPayment;
    private DtoOrderItemRequest itemRequest1;
    private DtoOrderItemRequest itemRequest2;
    private DtoAddress mappedShippingAddressDto; // For expected response
    private DtoAddress mappedBillingAddressDto;  // For expected response
    private DtoUserSummary mappedCustomerSummaryDto; // For expected response

    @BeforeEach
    void setUp() {
        // --- Basic Entities ---
        testCustomer = new Customer();
        testCustomer.setUserId(1L);
        testCustomer.setUsername("testuser");
        testCustomer.setFirstName("Test");
        testCustomer.setLastName("User");


        testShippingAddress = new Address();
        testShippingAddress.setAddressId(10L);
        testShippingAddress.setUser(testCustomer);
        testShippingAddress.setStreet("123 Ship St");
        testShippingAddress.setCity("Shiptown");
        testShippingAddress.setPostalCode("12345");
        testShippingAddress.setCountry("Testland");


        testBillingAddress = new Address();
        testBillingAddress.setAddressId(20L);
        testBillingAddress.setUser(testCustomer);
        testBillingAddress.setStreet("456 Bill Ave");
        testBillingAddress.setCity("Billville");
        testBillingAddress.setPostalCode("67890");
        testBillingAddress.setCountry("Testland");


        testProduct1 = new Product();
        testProduct1.setProductId(100L);
        testProduct1.setName("Product A");
        testProduct1.setPrice(BigDecimal.valueOf(10.00));
        testProduct1.setStockQuantity(50);

        testProduct2 = new Product();
        testProduct2.setProductId(200L);
        testProduct2.setName("Product B");
        testProduct2.setPrice(BigDecimal.valueOf(25.50));
        testProduct2.setStockQuantity(10);

        testPayment = new Payment();
        testPayment.setPaymentId(500L);
        testPayment.setStatus(Payment.PaymentStatus.PENDING);
        testPayment.setAmount(BigDecimal.valueOf(61.00)); // (1 * 10.00) + (2 * 25.50)

        // --- Order Entity ---
        testOrder = new Order();
        testOrder.setOrderId(1L);
        testOrder.setCustomer(testCustomer);
        testOrder.setShippingAddress(testShippingAddress);
        testOrder.setBillingAddress(testBillingAddress);
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(61.00));
        testOrder.setOrderNumber("ORD-12345");
        testOrder.setCreatedAt(LocalDateTime.now().minusDays(1));
        testOrder.setUpdatedAt(LocalDateTime.now());
        testOrder.setPayment(testPayment); // Link payment
        testPayment.setOrder(testOrder); // Bidirectional link

        OrderItem item1 = new OrderItem();
        item1.setOrderItemId(11L);
        item1.setOrder(testOrder);
        item1.setProduct(testProduct1);
        item1.setQuantity(1);
        item1.setPriceAtPurchase(testProduct1.getPrice());

        OrderItem item2 = new OrderItem();
        item2.setOrderItemId(12L);
        item2.setOrder(testOrder);
        item2.setProduct(testProduct2);
        item2.setQuantity(2);
        item2.setPriceAtPurchase(testProduct2.getPrice());

        testOrder.setItems(new ArrayList<>(List.of(item1, item2))); // Use ArrayList

        // --- DTOs ---
       // --- DTOs ---
        // Use setters for DtoOrderRequest
        itemRequest1 = new DtoOrderItemRequest(testProduct1.getProductId(), 1); // Assuming this has @AllArgsConstructor
        itemRequest2 = new DtoOrderItemRequest(testProduct2.getProductId(), 2); // Assuming this has @AllArgsConstructor
        testOrderRequest = new DtoOrderRequest();
        testOrderRequest.setShippingAddressId(testShippingAddress.getAddressId());
        testOrderRequest.setBillingAddressId(testBillingAddress.getAddressId());
        testOrderRequest.setItems(List.of(itemRequest1, itemRequest2));

        // --- Mapped DTOs (for expected response simulation) ---
        // Simulate what the mappers would produce for the expected response

        // Correct DtoProductSummary constructor call (provide all 7 args)
        DtoProductSummary productSummary1 = new DtoProductSummary(
                testProduct1.getProductId(), // productId (Long)
                testProduct1.getName(),      // name (String)
                testProduct1.getPrice(),     // price (BigDecimal)
                null,                        // primaryImageUrl (String) - Assuming null for test
                null,                        // averageRating (Double) - Assuming null for test
                null,                        // brand (String) - Assuming null for test
                null                         // model (String) - Assuming null for test
        );
        DtoProductSummary productSummary2 = new DtoProductSummary(
                testProduct2.getProductId(), // productId (Long)
                testProduct2.getName(),      // name (String)
                testProduct2.getPrice(),     // price (BigDecimal)
                null,                        // primaryImageUrl (String) - Assuming null for test
                null,                        // averageRating (Double) - Assuming null for test
                null,                        // brand (String) - Assuming null for test
                null                         // model (String) - Assuming null for test
        );

        // Use constructor for DtoOrderItem (assuming it has @AllArgsConstructor)
        DtoOrderItem orderItemDto1 = new DtoOrderItem(11L, 1, BigDecimal.valueOf(10.00), productSummary1);
        DtoOrderItem orderItemDto2 = new DtoOrderItem(12L, 2, BigDecimal.valueOf(25.50), productSummary2);

        // Use setters for DtoPaymentSummary
        DtoPaymentSummary paymentSummaryDto = new DtoPaymentSummary();
        paymentSummaryDto.setPaymentId(testPayment.getPaymentId());
        paymentSummaryDto.setAmount(testPayment.getAmount());
        paymentSummaryDto.setStatus(testPayment.getStatus());
        paymentSummaryDto.setPaymentMethod(null); // Assuming null initially
        paymentSummaryDto.setTransactionId(null); // Assuming null initially

        // Simulate mapped addresses (assuming DtoAddress has setters or appropriate constructor)
        mappedShippingAddressDto = new DtoAddress();
        // ... (setters for mappedShippingAddressDto as before) ...
        mappedShippingAddressDto.setAddressId(testShippingAddress.getAddressId());
        mappedShippingAddressDto.setStreet(testShippingAddress.getStreet());
        mappedShippingAddressDto.setCity(testShippingAddress.getCity());
        mappedShippingAddressDto.setPostalCode(testShippingAddress.getPostalCode());
        mappedShippingAddressDto.setCountry(testShippingAddress.getCountry());


        mappedBillingAddressDto = new DtoAddress();
        // ... (setters for mappedBillingAddressDto as before) ...
        mappedBillingAddressDto.setAddressId(testBillingAddress.getAddressId());
        mappedBillingAddressDto.setStreet(testBillingAddress.getStreet());
        mappedBillingAddressDto.setCity(testBillingAddress.getCity());
        mappedBillingAddressDto.setPostalCode(testBillingAddress.getPostalCode());
        mappedBillingAddressDto.setCountry(testBillingAddress.getCountry());


        // Simulate mapped customer summary (assuming constructor exists or use setters)
        mappedCustomerSummaryDto = new DtoUserSummary(testCustomer.getUserId(), testCustomer.getUsername(), testCustomer.getFirstName(), testCustomer.getLastName());


        // Use setters for the expected DtoOrderResponse
        expectedOrderResponse = new DtoOrderResponse();
        expectedOrderResponse.setOrderId(testOrder.getOrderId());
        // ... (set remaining fields for expectedOrderResponse using setters as before) ...
        expectedOrderResponse.setOrderNumber(testOrder.getOrderNumber());
        expectedOrderResponse.setStatus(testOrder.getStatus());
        expectedOrderResponse.setTotalAmount(testOrder.getTotalAmount());
        expectedOrderResponse.setCreatedAt(testOrder.getCreatedAt());
        expectedOrderResponse.setUpdatedAt(testOrder.getUpdatedAt());
        expectedOrderResponse.setShippingAddress(mappedShippingAddressDto);
        expectedOrderResponse.setBillingAddress(mappedBillingAddressDto);
        expectedOrderResponse.setItems(List.of(orderItemDto1, orderItemDto2));
        expectedOrderResponse.setPayment(paymentSummaryDto); // Use the DTO created with setters
        expectedOrderResponse.setShipments(Collections.emptyList());
        expectedOrderResponse.setCustomer(mappedCustomerSummaryDto);
    }


     @Test
    void createOrder_ShippingAddressNotFound() {
        // Arrange
        Long nonExistentAddressId = 998L;
        // Use setters for the request DTO
        DtoOrderRequest badRequest = new DtoOrderRequest();
        badRequest.setShippingAddressId(nonExistentAddressId);
        badRequest.setBillingAddressId(testBillingAddress.getAddressId());
        badRequest.setItems(testOrderRequest.getItems());

        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findByAddressIdAndUserUserId(nonExistentAddressId, testCustomer.getUserId())).thenReturn(Optional.empty());

        // Act & Assert
        AddressNotFoundException exception = assertThrows(AddressNotFoundException.class, () -> {
            orderService.createOrder(badRequest, testCustomer.getUserId());
        });
        assertEquals("Shipping address not found or doesn't belong to customer", exception.getMessage());
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(addressRepository).findByAddressIdAndUserUserId(nonExistentAddressId, testCustomer.getUserId());
        verifyNoMoreInteractions(addressRepository);
        verifyNoInteractions(productRepository, orderRepository, paymentService, orderMapper);
    }

     @Test
    void createOrder_BillingAddressNotFound() {
        // Arrange
        Long nonExistentAddressId = 997L;
        // Use setters for the request DTO
        DtoOrderRequest badRequest = new DtoOrderRequest();
        badRequest.setShippingAddressId(testShippingAddress.getAddressId());
        badRequest.setBillingAddressId(nonExistentAddressId);
        badRequest.setItems(testOrderRequest.getItems());

        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findByAddressIdAndUserUserId(testShippingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testShippingAddress));
        when(addressRepository.findByAddressIdAndUserUserId(nonExistentAddressId, testCustomer.getUserId())).thenReturn(Optional.empty());

        // Act & Assert
        AddressNotFoundException exception = assertThrows(AddressNotFoundException.class, () -> {
            orderService.createOrder(badRequest, testCustomer.getUserId());
        });
        assertEquals("Billing address not found or doesn't belong to customer", exception.getMessage());
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(addressRepository).findByAddressIdAndUserUserId(testShippingAddress.getAddressId(), testCustomer.getUserId());
        verify(addressRepository).findByAddressIdAndUserUserId(nonExistentAddressId, testCustomer.getUserId());
        verifyNoInteractions(productRepository, orderRepository, paymentService, orderMapper);
    }

     @Test
    void createOrder_ProductNotFound() {
        // Arrange
        Long nonExistentProductId = 996L;
        DtoOrderItemRequest badItemRequest = new DtoOrderItemRequest(nonExistentProductId, 1);
        // Use setters for the request DTO
        DtoOrderRequest badRequest = new DtoOrderRequest();
        badRequest.setShippingAddressId(testShippingAddress.getAddressId());
        badRequest.setBillingAddressId(testBillingAddress.getAddressId());
        badRequest.setItems(List.of(badItemRequest));


        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findByAddressIdAndUserUserId(testShippingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testShippingAddress));
        when(addressRepository.findByAddressIdAndUserUserId(testBillingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testBillingAddress));
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // Act & Assert
        ProductNotFoundException exception = assertThrows(ProductNotFoundException.class, () -> {
            orderService.createOrder(badRequest, testCustomer.getUserId());
        });
        assertEquals("Product with ID " + nonExistentProductId + " not found", exception.getMessage());
        verify(productRepository).findById(nonExistentProductId);
        verifyNoInteractions(orderRepository, paymentService, orderMapper);
    }

    @Test
    void createOrder_InsufficientStock() {
        // Arrange
        testProduct1.setStockQuantity(0); // No stock
        DtoOrderItemRequest itemRequest1 = new DtoOrderItemRequest(testProduct1.getProductId(), 1); // Request 1
         // Use setters for the request DTO
        DtoOrderRequest badRequest = new DtoOrderRequest();
        badRequest.setShippingAddressId(testShippingAddress.getAddressId());
        badRequest.setBillingAddressId(testBillingAddress.getAddressId());
        badRequest.setItems(List.of(itemRequest1));


        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findByAddressIdAndUserUserId(testShippingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testShippingAddress));
        when(addressRepository.findByAddressIdAndUserUserId(testBillingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testBillingAddress));
        when(productRepository.findById(testProduct1.getProductId())).thenReturn(Optional.of(testProduct1));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(badRequest, testCustomer.getUserId());
        });
        assertEquals("Insufficient stock for product ID: " + testProduct1.getProductId(), exception.getMessage());
        verify(productRepository).findById(testProduct1.getProductId());
        verify(productRepository, never()).save(any(Product.class)); // Stock not updated
        verifyNoInteractions(orderRepository, paymentService, orderMapper);
    }


    // --- createOrder_Success (Updated) ---
    @Test
    void createOrder_Success() {
        // Arrange
        when(customerRepository.findById(testCustomer.getUserId())).thenReturn(Optional.of(testCustomer));
        when(addressRepository.findByAddressIdAndUserUserId(testShippingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testShippingAddress));
        when(addressRepository.findByAddressIdAndUserUserId(testBillingAddress.getAddressId(), testCustomer.getUserId())).thenReturn(Optional.of(testBillingAddress));
        when(productRepository.findById(testProduct1.getProductId())).thenReturn(Optional.of(testProduct1));
        when(productRepository.findById(testProduct2.getProductId())).thenReturn(Optional.of(testProduct2));

        // Capture the Order argument when saved
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            // Simulate DB generating ID and order number
            savedOrder.setOrderId(99L);
            savedOrder.setOrderNumber("ORD-GENERATED");
            assertNotNull(savedOrder.getPayment(), "Payment should be associated before saving order");
            assertEquals(Payment.PaymentStatus.PENDING, savedOrder.getPayment().getStatus());
            assertEquals(savedOrder.getTotalAmount(), savedOrder.getPayment().getAmount());
            savedOrder.getPayment().setPaymentId(555L); // Simulate payment getting ID after save cascade
            return savedOrder;
        });

        // Mock the mapper to return the expected DTO based on the *captured* saved Order
        // Use a more flexible ArgumentCaptor or thenAnswer if exact matching is needed
        when(orderMapper.toDtoOrderResponse(any(Order.class))).thenAnswer(invocation -> {
             Order savedOrder = invocation.getArgument(0);
             // Create a response DTO based on the *actual* saved order data
             DtoOrderResponse response = new DtoOrderResponse();
             response.setOrderId(savedOrder.getOrderId()); // Use generated ID
             response.setOrderNumber(savedOrder.getOrderNumber()); // Use generated number
             response.setStatus(savedOrder.getStatus());
             response.setTotalAmount(savedOrder.getTotalAmount());
             response.setCreatedAt(savedOrder.getCreatedAt()); // Should be set by JPA
             response.setUpdatedAt(savedOrder.getUpdatedAt()); // Should be set by JPA
             // Assume mapper correctly maps nested objects - we use the pre-built expected ones for assertion matching
             response.setShippingAddress(expectedOrderResponse.getShippingAddress());
             response.setBillingAddress(expectedOrderResponse.getBillingAddress());
             response.setItems(expectedOrderResponse.getItems());
             response.setPayment(expectedOrderResponse.getPayment()); // Adjust if payment ID changes
             response.setShipments(expectedOrderResponse.getShipments());
             response.setCustomer(expectedOrderResponse.getCustomer());

             // Update the payment summary DTO within the response if its ID changed
             if (savedOrder.getPayment() != null && savedOrder.getPayment().getPaymentId() != null) {
                 DtoPaymentSummary paymentSummary = response.getPayment() != null ? response.getPayment() : new DtoPaymentSummary();
                 paymentSummary.setPaymentId(savedOrder.getPayment().getPaymentId());
                 response.setPayment(paymentSummary);
             }


             return response;
        });


        // Act
        DtoOrderResponse createdOrderDto = orderService.createOrder(testOrderRequest, testCustomer.getUserId());

        // Assert
        assertNotNull(createdOrderDto);
        assertEquals(99L, createdOrderDto.getOrderId()); // Check against simulated generated ID
        assertEquals("ORD-GENERATED", createdOrderDto.getOrderNumber());// Check against simulated generated number
        assertEquals(expectedOrderResponse.getStatus(), createdOrderDto.getStatus());
        assertEquals(expectedOrderResponse.getTotalAmount(), createdOrderDto.getTotalAmount());
        assertEquals(expectedOrderResponse.getItems().size(), createdOrderDto.getItems().size());
        assertNotNull(createdOrderDto.getPayment());
        assertEquals(555L, createdOrderDto.getPayment().getPaymentId());// Check simulated generated payment ID
        assertEquals(Payment.PaymentStatus.PENDING, createdOrderDto.getPayment().getStatus());
         assertEquals(expectedOrderResponse.getShippingAddress(), createdOrderDto.getShippingAddress());
         assertEquals(expectedOrderResponse.getBillingAddress(), createdOrderDto.getBillingAddress());
         assertEquals(expectedOrderResponse.getCustomer(), createdOrderDto.getCustomer());



        // Verify interactions
        verify(customerRepository).findById(testCustomer.getUserId());
        verify(addressRepository).findByAddressIdAndUserUserId(testShippingAddress.getAddressId(), testCustomer.getUserId());
        verify(addressRepository).findByAddressIdAndUserUserId(testBillingAddress.getAddressId(), testCustomer.getUserId());
        verify(productRepository).findById(testProduct1.getProductId());
        verify(productRepository).findById(testProduct2.getProductId());
        verify(productRepository).save(testProduct1); // Verify stock deduction save
        verify(productRepository).save(testProduct2); // Verify stock deduction save
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toDtoOrderResponse(any(Order.class));
        // Verify stock updated BEFORE save
        assertEquals(49, testProduct1.getStockQuantity());
        assertEquals(8, testProduct2.getStockQuantity());
    }


    // --- Other test methods using expectedOrderResponse for mocking the mapper ---

     @Test
    void getOrderById_Success_CustomerOwner() {
        // Arrange
        when(orderRepository.findById(testOrder.getOrderId())).thenReturn(Optional.of(testOrder));
        // Mock the mapper to return the pre-built expected DTO
        when(orderMapper.toDtoOrderResponse(testOrder)).thenReturn(expectedOrderResponse);

        // Act
        DtoOrderResponse foundOrder = orderService.getOrderById(testOrder.getOrderId(), testCustomer.getUserId(), "CUSTOMER");

        // Assert
        assertNotNull(foundOrder);
        // Assert against the expected DTO fields
        assertEquals(expectedOrderResponse.getOrderId(), foundOrder.getOrderId());
        assertEquals(expectedOrderResponse.getOrderNumber(), foundOrder.getOrderNumber());
        assertEquals(expectedOrderResponse.getCustomer().getUserId(), foundOrder.getCustomer().getUserId());

        verify(orderRepository).findById(testOrder.getOrderId());
        verify(orderMapper).toDtoOrderResponse(testOrder);
    }

    // ... (update other tests similarly where DtoOrderResponse is instantiated or mocked)
    // Make sure to use 'expectedOrderResponse' when setting up mock returns from the mapper
    // e.g., in updateOrderStatus_Success, cancelOrder_Success...


     @Test
    void updateOrderStatus_Success() {
        // Arrange
        Long adminUserId = 55L;
        Order.OrderStatus newStatus = Order.OrderStatus.PROCESSING;
        // Update the status in the original testOrder entity before mocking save/map
        testOrder.setStatus(newStatus);

        when(orderRepository.findById(testOrder.getOrderId())).thenReturn(Optional.of(testOrder)); // Return the order to be updated
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder); // Assume save returns the updated entity

        // Mock the mapper to return an updated response DTO
        when(orderMapper.toDtoOrderResponse(any(Order.class))).thenAnswer(invocation -> {
             Order updatedOrder = invocation.getArgument(0);
             // Create/update the response DTO based on the updated order
             DtoOrderResponse updatedResponse = new DtoOrderResponse();
             // Copy fields from expectedOrderResponse and update the status
             updatedResponse.setOrderId(updatedOrder.getOrderId());
             updatedResponse.setOrderNumber(updatedOrder.getOrderNumber());
             updatedResponse.setStatus(updatedOrder.getStatus()); // Use the NEW status
             updatedResponse.setTotalAmount(updatedOrder.getTotalAmount());
             updatedResponse.setCreatedAt(updatedOrder.getCreatedAt());
             updatedResponse.setUpdatedAt(updatedOrder.getUpdatedAt()); // Should be updated by JPA
             updatedResponse.setShippingAddress(expectedOrderResponse.getShippingAddress());
             updatedResponse.setBillingAddress(expectedOrderResponse.getBillingAddress());
             updatedResponse.setItems(expectedOrderResponse.getItems());
             updatedResponse.setPayment(expectedOrderResponse.getPayment());
             updatedResponse.setShipments(expectedOrderResponse.getShipments());
             updatedResponse.setCustomer(expectedOrderResponse.getCustomer());
             assertEquals(newStatus, updatedOrder.getStatus()); // Verify status in entity passed to mapper
             return updatedResponse;
        });


        // Act
        DtoOrderResponse updatedOrderDto = orderService.updateOrderStatus(testOrder.getOrderId(), newStatus, adminUserId);

        // Assert
        assertNotNull(updatedOrderDto);
        assertEquals(newStatus, updatedOrderDto.getStatus()); // Check the status in the returned DTO
        assertEquals(testOrder.getOrderId(), updatedOrderDto.getOrderId());

        verify(orderRepository).findById(testOrder.getOrderId());
        verify(orderRepository).save(testOrder); // Verify the specific order object was saved
        // Verify the mapper was called with the order *after* its status was set
        verify(orderMapper).toDtoOrderResponse(argThat(order -> order.getStatus() == newStatus));
    }


     @Test
    void cancelOrder_Success_Customer_NoRefundNeeded() {
        // Arrange
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setPayment(null); // No payment -> no refund
        int initialStock1 = testProduct1.getStockQuantity();
        int initialStock2 = testProduct2.getStockQuantity();

        when(orderRepository.findById(testOrder.getOrderId())).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder); // save returns the updated (cancelled) order

        // Mock the mapper for the cancelled order
        when(orderMapper.toDtoOrderResponse(any(Order.class))).thenAnswer(invocation -> {
             Order cancelledOrder = invocation.getArgument(0);
             assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus());
             // Create a response reflecting the cancelled state
             DtoOrderResponse cancelledResponse = new DtoOrderResponse();
             // Copy fields from expectedOrderResponse and update status
             cancelledResponse.setOrderId(cancelledOrder.getOrderId());
             cancelledResponse.setOrderNumber(cancelledOrder.getOrderNumber());
             cancelledResponse.setStatus(Order.OrderStatus.CANCELLED); // Set cancelled status
             cancelledResponse.setTotalAmount(cancelledOrder.getTotalAmount());
             cancelledResponse.setCreatedAt(cancelledOrder.getCreatedAt());
             cancelledResponse.setUpdatedAt(cancelledOrder.getUpdatedAt()); // Should be updated
             cancelledResponse.setShippingAddress(expectedOrderResponse.getShippingAddress());
             cancelledResponse.setBillingAddress(expectedOrderResponse.getBillingAddress());
             cancelledResponse.setItems(expectedOrderResponse.getItems());
             cancelledResponse.setPayment(null); // Payment was null
             cancelledResponse.setShipments(expectedOrderResponse.getShipments());
             cancelledResponse.setCustomer(expectedOrderResponse.getCustomer());
             return cancelledResponse;
        });

        // Act
        DtoOrderResponse cancelledOrderDto = orderService.cancelOrder(testOrder.getOrderId(), testCustomer.getUserId(), "CUSTOMER");

        // Assert
        assertNotNull(cancelledOrderDto);
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrderDto.getStatus());
        assertEquals(initialStock1 + 1, testProduct1.getStockQuantity());
        assertEquals(initialStock2 + 2, testProduct2.getStockQuantity());
        verify(productRepository).save(testProduct1);
        verify(productRepository).save(testProduct2);
        verify(orderRepository).findById(testOrder.getOrderId());
        verify(orderRepository).save(argThat(order -> order.getStatus() == Order.OrderStatus.CANCELLED));
        verify(orderMapper).toDtoOrderResponse(argThat(order -> order.getStatus() == Order.OrderStatus.CANCELLED));
        verifyNoInteractions(paymentService);
    }

    // ... Ensure other tests calling cancelOrder, getOrderById, etc.
    // ... also mock orderMapper.toDtoOrderResponse correctly, possibly using
    // ... the pre-built 'expectedOrderResponse' and adjusting status if necessary.

}