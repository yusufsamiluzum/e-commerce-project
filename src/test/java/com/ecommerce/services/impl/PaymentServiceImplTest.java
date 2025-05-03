package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoPaymentInitiationResponse;
import com.ecommerce.entities.Payment;
import com.ecommerce.entities.Payment.PaymentMethod;
import com.ecommerce.entities.Payment.PaymentStatus;
import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;

// --- Stripe Imports for Mocking ---
import com.stripe.Stripe;
import com.stripe.exception.ApiException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils; // For setting @Value fields

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    // Mocks for Stripe static methods
    private MockedStatic<Stripe> mockedStripe;
    private MockedStatic<PaymentIntent> mockedPaymentIntent;
    private MockedStatic<Webhook> mockedWebhook;
    private MockedStatic<Refund> mockedRefund;


    // Test Data
    private Customer customer;
    private Order order;
    private Payment payment;
    private final Long orderId = 1L;
    private final Long paymentId = 10L;
    private final Long customerId = 100L;
    private final BigDecimal amount = new BigDecimal("150.00");
    private final String stripeClientSecret = "pi_test_secret_123";
    private final String stripePaymentIntentId = "pi_test_123";
    private final String stripeRefundId = "re_test_456";
    private final String stripeWebhookSecret = "whsec_test_secret";
    private final String stripeApiKey = "sk_test_key";


    @BeforeEach
    void setUp() {
        // Initialize test data
        customer = new Customer();
        customer.setUserId(customerId);

        order = new Order();
        order.setOrderId(orderId);
        order.setCustomer(customer);
        order.setTotalAmount(amount);
        order.setStatus(Order.OrderStatus.PENDING);

        payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setOrder(order);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        // payment.setGatewayTransactionId(null); // Initially null or set later
        // payment.setPaymentMethod(null); // Set during initiation

        // --- Mock Static Stripe Methods ---
        // Use try-with-resources or manage closing manually in @AfterEach
        mockedStripe = mockStatic(Stripe.class);
        mockedPaymentIntent = mockStatic(PaymentIntent.class);
        mockedWebhook = mockStatic(Webhook.class);
        mockedRefund = mockStatic(Refund.class);


        // --- Configure @Value fields ---
        // Use ReflectionTestUtils to inject values into private fields
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", stripeApiKey, String.class);
        ReflectionTestUtils.setField(paymentService, "stripeWebhookSecret", stripeWebhookSecret, String.class);

        // Call init method manually AFTER setting the key
        paymentService.init();
        mockedStripe.verify(() -> { Stripe.apiKey = stripeApiKey; });

    }

     @AfterEach
    void tearDown() {
        // Close static mocks to prevent test interference
        if (mockedStripe != null) mockedStripe.close();
        if (mockedPaymentIntent != null) mockedPaymentIntent.close();
        if (mockedWebhook != null) mockedWebhook.close();
        if (mockedRefund != null) mockedRefund.close();
    }


    // --- Tests for initiatePayment ---

    @Test
    void initiatePayment_Stripe_Success() throws StripeException {
        // Arrange
        when(paymentRepository.findByOrderOrderId(orderId)).thenReturn(Optional.of(payment));

        // Mock Stripe PaymentIntent creation
        PaymentIntent mockPI = mock(PaymentIntent.class);
        when(mockPI.getId()).thenReturn(stripePaymentIntentId);
        when(mockPI.getClientSecret()).thenReturn(stripeClientSecret);
        mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                           .thenReturn(mockPI);

        // Act
        DtoPaymentInitiationResponse response = paymentService.initiatePayment(orderId, PaymentMethod.STRIPE, customerId);

        // Assert
        assertNotNull(response);
        assertEquals(paymentId, response.getPaymentId());
        assertEquals(PaymentMethod.STRIPE, response.getPaymentMethod());
        assertEquals(stripeClientSecret, response.getClientSecret());
        assertNull(response.getPaypalOrderId()); // Ensure PayPal ID is null for Stripe
        assertEquals(amount, response.getAmount());
        assertEquals("usd", response.getCurrency()); // Assuming USD

        // Verify interactions
        verify(paymentRepository).findByOrderOrderId(orderId);
        verify(paymentRepository).save(payment); // Should save after setting method and PI ID
        mockedPaymentIntent.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)));

        // Check Payment entity updates
        assertEquals(PaymentMethod.STRIPE, payment.getPaymentMethod());
        assertEquals(stripePaymentIntentId, payment.getGatewayTransactionId());
        assertEquals(PaymentStatus.PENDING, payment.getStatus()); // Status shouldn't change here
    }

     @Test
    void initiatePayment_PayPal_Success_NotImplemented() {
         // Arrange
        when(paymentRepository.findByOrderOrderId(orderId)).thenReturn(Optional.of(payment));

        // Act
        DtoPaymentInitiationResponse response = paymentService.initiatePayment(orderId, PaymentMethod.PAYPAL, customerId);

         // Assert (Based on current placeholder implementation)
         assertNotNull(response);
         assertEquals(paymentId, response.getPaymentId());
         assertEquals(PaymentMethod.PAYPAL, response.getPaymentMethod());
         assertNull(response.getClientSecret()); // Ensure Stripe secret is null for PayPal
         assertEquals("PAYPAL_ORDER_ID_PLACEHOLDER", response.getPaypalOrderId());
         assertEquals(amount, response.getAmount());
         assertEquals("usd", response.getCurrency());

         // Verify interactions
         verify(paymentRepository).findByOrderOrderId(orderId);
         verify(paymentRepository).save(payment); // Saves with placeholder ID

         // Check Payment entity updates
         assertEquals(PaymentMethod.PAYPAL, payment.getPaymentMethod());
         assertEquals("PAYPAL_ORDER_ID_PLACEHOLDER", payment.getGatewayTransactionId());
         assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void initiatePayment_PaymentNotFound() {
        // Arrange
        when(paymentRepository.findByOrderOrderId(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        PaymentNotFoundException exception = assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.initiatePayment(orderId, PaymentMethod.STRIPE, customerId);
        });
        assertEquals("Payment record not found for order ID: " + orderId, exception.getMessage());
        verify(paymentRepository).findByOrderOrderId(orderId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_UnauthorizedAccess() {
        // Arrange
        Long wrongCustomerId = 999L;
        when(paymentRepository.findByOrderOrderId(orderId)).thenReturn(Optional.of(payment));

        // Act & Assert
        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            paymentService.initiatePayment(orderId, PaymentMethod.STRIPE, wrongCustomerId);
        });
        assertEquals("User not authorized for this payment", exception.getMessage());
        verify(paymentRepository).findByOrderOrderId(orderId);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_PaymentNotInPendingState() {
        // Arrange
        payment.setStatus(PaymentStatus.SUCCESS); // Set to a non-pending state
        when(paymentRepository.findByOrderOrderId(orderId)).thenReturn(Optional.of(payment));

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.initiatePayment(orderId, PaymentMethod.STRIPE, customerId);
        });
        assertTrue(exception.getMessage().contains("Payment is not in PENDING state"));
        verify(paymentRepository).findByOrderOrderId(orderId);
        verify(paymentRepository, never()).save(any());
    }

     @Test
    void initiatePayment_StripeApiException() {
        // Arrange
        when(paymentRepository.findByOrderOrderId(orderId)).thenReturn(Optional.of(payment));
        ApiException stripeApiException = mock(ApiException.class); // More specific exception if needed
        when(stripeApiException.getMessage()).thenReturn("Stripe API error");

        mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                           .thenThrow(stripeApiException);

        // Act & Assert
        PaymentException exception = assertThrows(PaymentException.class, () -> {
            paymentService.initiatePayment(orderId, PaymentMethod.STRIPE, customerId);
        });
        assertTrue(exception.getMessage().contains("Failed to initiate payment: Stripe API error"));
        verify(paymentRepository).findByOrderOrderId(orderId);
        verify(paymentRepository, never()).save(any(Payment.class)); // Save shouldn't happen on PI creation failure
    }


    // --- Tests for handleStripeWebhook ---

    @Test
    void handleStripeWebhook_PaymentIntentSucceeded() throws SignatureVerificationException {
        // Arrange
        String payload = "{\"id\": \"evt_test_webhook\",\"object\": \"event\",\"type\": \"payment_intent.succeeded\"}"; // Simplified payload
        String sigHeader = "t=timestamp,v1=signature"; // Dummy header

        Event mockEvent = mock(Event.class);
        PaymentIntent mockPI = mock(PaymentIntent.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPI));
        when(mockPI.getMetadata()).thenReturn(Map.of("payment_id", paymentId.toString(), "order_id", orderId.toString()));
        when(mockPI.getId()).thenReturn(stripePaymentIntentId); // The transaction ID

        mockedWebhook.when(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret))
                     .thenReturn(mockEvent);

        // Mock repository fetches for updatePaymentStatus
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment)); // Payment is initially PENDING
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order)); // Order is initially PENDING

        // Act
        paymentService.handleStripeWebhook(payload, sigHeader);

        // Assert
        // Verify status updates
        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
        assertEquals(stripePaymentIntentId, payment.getGatewayTransactionId()); // Updated transaction ID
        assertEquals(Order.OrderStatus.PROCESSING, order.getStatus()); // Order status updated

        // Verify repository calls
        mockedWebhook.verify(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret));
        verify(paymentRepository).findById(paymentId);
        verify(orderRepository).findById(orderId);
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
    }

    @Test
    void handleStripeWebhook_PaymentIntentFailed() throws SignatureVerificationException {
         // Arrange
        String payload = "{\"id\": \"evt_test_webhook\",\"object\": \"event\",\"type\": \"payment_intent.payment_failed\"}";
        String sigHeader = "t=timestamp,v1=signature";

        Event mockEvent = mock(Event.class);
        PaymentIntent mockPI = mock(PaymentIntent.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

        when(mockEvent.getType()).thenReturn("payment_intent.payment_failed");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPI));
        when(mockPI.getMetadata()).thenReturn(Map.of("payment_id", paymentId.toString(), "order_id", orderId.toString()));
        when(mockPI.getId()).thenReturn(stripePaymentIntentId); // The transaction ID
        // Optional: Mock getLastPaymentError if needed for logging
        // when(mockPI.getLastPaymentError()).thenReturn(mock(PaymentIntent.Error.class));
        // when(mockPI.getLastPaymentError().getMessage()).thenReturn("Card declined");


        mockedWebhook.when(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret))
                     .thenReturn(mockEvent);

        // Mock repository fetches
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment)); // Payment is PENDING
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order)); // Order is PENDING

        // Act
        paymentService.handleStripeWebhook(payload, sigHeader);

        // Assert
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(stripePaymentIntentId, payment.getGatewayTransactionId());
        assertEquals(Order.OrderStatus.CANCELLED, order.getStatus()); // Check if order becomes cancelled

        // Verify repository calls
        mockedWebhook.verify(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret));
        verify(paymentRepository).findById(paymentId);
        verify(orderRepository).findById(orderId);
        verify(paymentRepository).save(payment);
        verify(orderRepository).save(order);
    }

    @Test
    void handleStripeWebhook_InvalidSignature() {
        // Arrange
        String payload = "{}";
        String sigHeader = "invalid_signature";
        SignatureVerificationException sigException = mock(SignatureVerificationException.class);
        when(sigException.getMessage()).thenReturn("Invalid signature");


        mockedWebhook.when(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret))
                     .thenThrow(sigException);

        // Act & Assert
        WebhookVerificationException thrown = assertThrows(WebhookVerificationException.class, () -> {
            paymentService.handleStripeWebhook(payload, sigHeader);
        });
        assertEquals("Invalid Stripe signature", thrown.getMessage());
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

     @Test
    void handleStripeWebhook_PaymentAlreadyProcessed_Success() throws SignatureVerificationException {
         // Arrange: Payment and Order are already in SUCCESS/PROCESSING state
        payment.setStatus(PaymentStatus.SUCCESS);
        order.setStatus(Order.OrderStatus.PROCESSING);

        String payload = "{\"id\": \"evt_test_webhook\",\"object\": \"event\",\"type\": \"payment_intent.succeeded\"}";
        String sigHeader = "t=timestamp,v1=signature";

        Event mockEvent = mock(Event.class);
        PaymentIntent mockPI = mock(PaymentIntent.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPI));
        when(mockPI.getMetadata()).thenReturn(Map.of("payment_id", paymentId.toString(), "order_id", orderId.toString()));
        when(mockPI.getId()).thenReturn(stripePaymentIntentId);

        mockedWebhook.when(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret))
                     .thenReturn(mockEvent);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        // Don't need to mock orderRepository.findById if payment update is skipped

        // Act
        paymentService.handleStripeWebhook(payload, sigHeader);

        // Assert: Verify that save methods were NOT called because status matched
        verify(paymentRepository).findById(paymentId); // Still need to find it
        verify(paymentRepository, never()).save(payment);
        verify(orderRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

     @Test
    void handleStripeWebhook_MissingMetadata() throws SignatureVerificationException {
        // Arrange
        String payload = "{\"id\": \"evt_test_webhook\",\"object\": \"event\",\"type\": \"payment_intent.succeeded\"}";
        String sigHeader = "t=timestamp,v1=signature";

        Event mockEvent = mock(Event.class);
        PaymentIntent mockPI = mock(PaymentIntent.class);
        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);

        when(mockEvent.getType()).thenReturn("payment_intent.succeeded");
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.of(mockPI));
        when(mockPI.getMetadata()).thenReturn(Map.of()); // EMPTY metadata
        when(mockPI.getId()).thenReturn(stripePaymentIntentId);

        mockedWebhook.when(() -> Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret))
                     .thenReturn(mockEvent);
        // No need to mock repositories as updatePaymentStatus should exit early

        // Act
        paymentService.handleStripeWebhook(payload, sigHeader);

        // Assert: Verify no updates happened
        verify(paymentRepository, never()).findById(anyLong());
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any());
        // Check logs (if possible/needed) or just assert no state change occurred indirectly
        assertEquals(PaymentStatus.PENDING, payment.getStatus()); // Should remain unchanged
    }


    // --- Tests for initiateRefund ---

    @Test
    void initiateRefund_Stripe_Success() throws StripeException {
        // Arrange
        payment.setStatus(PaymentStatus.SUCCESS); // Must be SUCCESS to refund
        payment.setPaymentMethod(PaymentMethod.STRIPE);
        payment.setGatewayTransactionId(stripePaymentIntentId); // Set the PI ID needed for refund

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Mock Stripe Refund creation
        Refund mockStripeRefund = mock(Refund.class);
        when(mockStripeRefund.getId()).thenReturn(stripeRefundId);
        when(mockStripeRefund.getStatus()).thenReturn("succeeded"); // or "pending"
        mockedRefund.when(() -> Refund.create(any(RefundCreateParams.class)))
                    .thenReturn(mockStripeRefund);

        // Act
        paymentService.initiateRefund(paymentId);

        // Assert
        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
        assertEquals(stripeRefundId, payment.getRefundTransactionId()); // Check if refund ID is stored

        // Verify interactions
        verify(paymentRepository).findById(paymentId);
        mockedRefund.verify(() -> Refund.create(any(RefundCreateParams.class)));
        verify(paymentRepository).save(payment);
    }

    @Test
    void initiateRefund_PaymentNotFound() {
        // Arrange
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // Act & Assert
        PaymentNotFoundException exception = assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.initiateRefund(paymentId);
        });
        assertEquals("Payment not found with ID: " + paymentId, exception.getMessage());
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository, never()).save(any());
        mockedRefund.verify(() -> Refund.create(any(RefundCreateParams.class)), never());
    }

    @Test
    void initiateRefund_PaymentNotSuccessful() {
        // Arrange
        payment.setStatus(PaymentStatus.PENDING); // Not in SUCCESS state
        payment.setPaymentMethod(PaymentMethod.STRIPE);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Act & Assert
        RefundException exception = assertThrows(RefundException.class, () -> {
            paymentService.initiateRefund(paymentId);
        });
        assertTrue(exception.getMessage().contains("Cannot refund payment that is not in SUCCESS status"));
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository, never()).save(any());
        mockedRefund.verify(() -> Refund.create(any(RefundCreateParams.class)), never());
    }

    @Test
    void initiateRefund_Stripe_MissingPaymentIntentId() {
         // Arrange
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.STRIPE);
        payment.setGatewayTransactionId(null); // Missing PI ID
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Act & Assert
        RefundException exception = assertThrows(RefundException.class, () -> {
            paymentService.initiateRefund(paymentId);
        });
        assertTrue(exception.getMessage().contains("PaymentIntent ID not found or invalid"));
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository, never()).save(any());
        mockedRefund.verify(() -> Refund.create(any(RefundCreateParams.class)), never());
    }

     @Test
    void initiateRefund_StripeApiException() {
        // Arrange
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(PaymentMethod.STRIPE);
        payment.setGatewayTransactionId(stripePaymentIntentId);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        ApiException stripeApiException = mock(ApiException.class);
        when(stripeApiException.getMessage()).thenReturn("Stripe refund error");
         mockedRefund.when(() -> Refund.create(any(RefundCreateParams.class)))
                    .thenThrow(stripeApiException);

        // Act & Assert
        RefundException exception = assertThrows(RefundException.class, () -> {
            paymentService.initiateRefund(paymentId);
        });
        assertTrue(exception.getMessage().contains("Failed to initiate refund: Stripe refund error"));
        verify(paymentRepository).findById(paymentId);
        verify(paymentRepository, never()).save(any()); // Save shouldn't happen on refund creation failure
    }

     @Test
    void initiateRefund_UnsupportedMethod() {
         // Arrange
         // Simulate an unsupported method if enum had more values, or test the else block directly
         // For now, we test that PayPal (which is unimplemented for refund) throws the correct exception implicitly.
         payment.setStatus(PaymentStatus.SUCCESS);
         payment.setPaymentMethod(PaymentMethod.PAYPAL); // PayPal refund not implemented
         payment.setGatewayTransactionId("some_paypal_id"); // Assume ID exists
         when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // Act & Assert
        // Since PayPal refund part only logs a warning and doesn't throw,
        // we expect no exception here based *purely* on the current service code.
        // If the intention WAS to throw for unimplemented PayPal refund, the service needs adjustment.
        // Let's test the theoretical "else" block if a third method existed.
        // We can't easily mock adding a new enum value, so this case is hard to test directly
        // without modifying the enum for the test.

        // Re-testing PayPal specifically for the *lack* of throwing an exception (based on current code)
         assertDoesNotThrow(() -> {
             paymentService.initiateRefund(paymentId);
             // If the service *should* throw for unimplemented PayPal, this test would fail.
         });
        // Verify it found the payment but didn't save (as nothing happened for PayPal)
         verify(paymentRepository).findById(paymentId);
         verify(paymentRepository, never()).save(payment); // No status change for unimplemented PayPal refund
    }

     // TODO: Add tests for handlePayPalWebhook once implemented
     // @Test
     // void handlePayPalWebhook_OrderApproved() { ... }
     // @Test
     // void handlePayPalWebhook_InvalidSignature() { ... }

}
