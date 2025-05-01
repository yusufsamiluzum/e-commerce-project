package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoPaymentInitiationResponse;
import com.ecommerce.entities.Payment;
import com.ecommerce.entities.order.Order; // Assuming OrderStatus lives here
import com.ecommerce.exceptions.*;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.PaymentRepository;
import com.ecommerce.services.PaymentService;

// --- Stripe Imports ---
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;



import jakarta.annotation.PostConstruct; // For setting API key
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map; // For PayPal headers

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    // Inject Mappers if needed for DTO conversion

    // --- API Keys and Secrets (Load from application.properties/.yml) ---
    @Value("${stripe.apiKey.secret}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    // --- Add PayPal credentials similarly ---
    // @Value("${paypal.clientId}")
    // private String paypalClientId;
    // @Value("${paypal.clientSecret}")
    // private String paypalClientSecret;
    // @Value("${paypal.webhook.id}")
    // private String paypalWebhookId;

    // Configure Stripe API key on startup
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        // Configure PayPal Client similarly
    }

    @Override
    @Transactional
    public DtoPaymentInitiationResponse initiatePayment(Long orderId, Payment.PaymentMethod paymentMethod, Long customerId) {
        log.info("Initiating payment for orderId: {}, method: {}", orderId, paymentMethod);
        Payment payment = paymentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment record not found for order ID: " + orderId));

        // Authorization: Check if payment's order belongs to the customer
        if (!payment.getOrder().getCustomer().getUserId().equals(customerId)) {
            throw new UnauthorizedAccessException("User not authorized for this payment");
        }

        // Validation: Check if payment is PENDING
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new PaymentException("Payment is not in PENDING state. Current status: " + payment.getStatus());
        }

        payment.setPaymentMethod(paymentMethod); // Set the chosen method

        try {
            if (paymentMethod == Payment.PaymentMethod.STRIPE) {
                // --- Stripe PaymentIntent Creation ---
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Amount in cents
                        .setCurrency("usd") // Or get from config/order
                        .addPaymentMethodType("card") // Or other types
                        .putMetadata("order_id", payment.getOrder().getOrderId().toString())
                        .putMetadata("payment_id", payment.getPaymentId().toString())
                        // .setCustomer(stripeCustomerId) // Optional: If you manage Stripe Customers
                        .build();

                PaymentIntent paymentIntent = PaymentIntent.create(params);
                log.info("Stripe PaymentIntent created: {}", paymentIntent.getId());

                // Store gateway ID and save payment
                payment.setGatewayTransactionId(paymentIntent.getId()); // Add this field to Payment entity
                paymentRepository.save(payment);

                return DtoPaymentInitiationResponse.builder()
                        .paymentId(payment.getPaymentId())
                        .paymentMethod(paymentMethod)
                        .clientSecret(paymentIntent.getClientSecret())
                        .amount(payment.getAmount())
                        .currency("usd")
                        .build();

            } else if (paymentMethod == Payment.PaymentMethod.PAYPAL) {
                // --- PayPal Order Creation ---
                log.warn("PayPal integration not fully implemented.");
                // 1. Build PayPal OrderRequest object (amount, currency, items, etc.)
                // 2. Use PayPalHttpClient to execute OrdersCreateRequest
                // 3. Get PayPal Order ID from the response
                // 4. Store PayPal Order ID in payment.setGatewayTransactionId()
                // 5. Save payment
                // 6. Return DtoPaymentInitiationResponse with paypalOrderId
                // Example Placeholder:
                 payment.setGatewayTransactionId("PAYPAL_ORDER_ID_PLACEHOLDER"); // Add gatewayTransactionId field to Payment entity
                 paymentRepository.save(payment);
                 return DtoPaymentInitiationResponse.builder()
                        .paymentId(payment.getPaymentId())
                        .paymentMethod(paymentMethod)
                        .paypalOrderId("PAYPAL_ORDER_ID_PLACEHOLDER") // Replace with actual ID
                        .amount(payment.getAmount())
                        .currency("usd")
                        .build();

            } else {
                throw new PaymentException("Unsupported payment method: " + paymentMethod);
            }
        } catch (Exception e) {
            log.error("Payment initiation failed for orderId: {}", orderId, e);
            throw new PaymentException("Failed to initiate payment: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            // --- Verify webhook signature ---
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe webhook signature.", e);
            throw new WebhookVerificationException("Invalid Stripe signature");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook payload.", e);
            throw new PaymentException("Webhook processing error: " + e.getMessage());
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new PaymentException("Could not deserialize Stripe event data object"));

        log.info("Received Stripe event: type={}, id={}", event.getType(), event.getId());

        // Handle specific event types
        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent paymentIntentSucceeded = (PaymentIntent) stripeObject;
                log.info("PaymentIntent succeeded: {}", paymentIntentSucceeded.getId());
                updatePaymentStatus(paymentIntentSucceeded.getMetadata().get("payment_id"),
                                    Payment.PaymentStatus.SUCCESS,
                                    paymentIntentSucceeded.getId(), // Use PI ID as transaction ID for now
                                    paymentIntentSucceeded.getMetadata().get("order_id"));
                break;
            case "payment_intent.payment_failed":
                PaymentIntent paymentIntentFailed = (PaymentIntent) stripeObject;
                log.warn("PaymentIntent failed: id={}, reason={}", paymentIntentFailed.getId(), paymentIntentFailed.getLastPaymentError() != null ? paymentIntentFailed.getLastPaymentError().getMessage() : "N/A");
                updatePaymentStatus(paymentIntentFailed.getMetadata().get("payment_id"),
                                    Payment.PaymentStatus.FAILED,
                                    paymentIntentFailed.getId(), // Use PI ID
                                    paymentIntentFailed.getMetadata().get("order_id"));
                break;
            // Add other event types if needed (e.g., refunds)
            // case "charge.refunded":
            //     Refund refund = (Refund) stripeObject;
            //     // Handle refund notification...
            //     break;
            default:
                log.warn("Unhandled Stripe event type: {}", event.getType());
        }
    }

     @Override
     @Transactional
     public void handlePayPalWebhook(String payload, Map<String, String> headers) {
         log.warn("PayPal webhook handling not fully implemented.");
         // --- Verify PayPal webhook signature ---
         // 1. Use PayPal SDK's WebhooksApi or similar to verify the signature
         //    You'll need paypalWebhookId, headers, and payload.
         // boolean isValid = verifyPayPalSignature(payload, headers);
         // if (!isValid) {
         //    throw new WebhookVerificationException("Invalid PayPal signature");
         // }

         // --- Parse Payload ---
         // 2. Parse the JSON payload into a PayPal Event object or Map.

         // --- Handle Event Type ---
         // 3. Check event_type (e.g., "CHECKOUT.ORDER.APPROVED", "CHECKOUT.ORDER.COMPLETED", "PAYMENT.CAPTURE.COMPLETED", "PAYMENT.CAPTURE.REFUNDED")

         // 4. Extract relevant data (PayPal order ID, capture ID, transaction ID, status) from the event's resource object.

         // 5. Find your internal Payment record using the PayPal Order ID stored in gatewayTransactionId or metadata.

         // 6. Update Payment status, transactionId, etc.
         // updatePaymentStatus(internalPaymentId, newStatus, paypalTransactionId, internalOrderId);
     }

    // Helper method to update payment and potentially order status
    private void updatePaymentStatus(String paymentIdStr, Payment.PaymentStatus status, String transactionId, String orderIdStr) {
        if (paymentIdStr == null || orderIdStr == null) {
             log.error("Missing payment_id or order_id in webhook metadata. Cannot update status.");
             return;
         }
        try {
            Long paymentId = Long.parseLong(paymentIdStr);
            Long orderId = Long.parseLong(orderIdStr);

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("Payment not found for ID from webhook: " + paymentId));

            // Avoid reprocessing or moving from SUCCESS/FAILED state unnecessarily
            if (payment.getStatus() == status) {
                 log.warn("Payment {} already has status {}. Skipping update.", paymentId, status);
                 return;
             }
             if (payment.getStatus() == Payment.PaymentStatus.SUCCESS || payment.getStatus() == Payment.PaymentStatus.FAILED) {
                  log.warn("Payment {} is already in a terminal state ({}). Received webhook for status {}. Check for potential issues.", paymentId, payment.getStatus(), status);
                 // Decide if you want to overwrite or log an error
                  // return; // Possibly skip update if already terminal
             }


            payment.setStatus(status);
            payment.setGatewayTransactionId(transactionId); // Store the actual transaction ID from gateway
            paymentRepository.save(payment);
            log.info("Updated payment {} status to {}", paymentId, status);

            // --- Update Order Status (Important!) ---
            Order order = orderRepository.findById(orderId)
                     .orElseThrow(() -> new OrderNotFoundException("Order not found for ID from webhook: " + orderId));

            if (status == Payment.PaymentStatus.SUCCESS) {
                order.setStatus(Order.OrderStatus.PROCESSING); // Or PAID, ready for shipment
                 log.info("Updating order {} status to PROCESSING", orderId);
            } else if (status == Payment.PaymentStatus.FAILED) {
                order.setStatus(Order.OrderStatus.CANCELLED);
                 log.warn("Updating order {} status to PAYMENT_FAILED", orderId);
                 // Optional: Add stock back here if payment failed after stock decrease in OrderService
                 // This requires careful consideration of transaction boundaries and idempotency
            }
            orderRepository.save(order);

            // Optional: Publish events (e.g., PaymentSuccessEvent) for further processing

        } catch (NumberFormatException e) {
            log.error("Invalid payment_id ({}) or order_id ({}) in webhook metadata.", paymentIdStr, orderIdStr, e);
        } catch (PaymentNotFoundException | OrderNotFoundException e) {
            log.error("Error finding payment or order during webhook processing: {}", e.getMessage(), e);
        } catch (Exception e) {
             log.error("Unexpected error during status update for payment {}: {}", paymentIdStr, e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public void initiateRefund(Long paymentId) {
        log.info("Initiating refund for paymentId: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS) {
            throw new RefundException("Cannot refund payment that is not in SUCCESS status. Current status: " + payment.getStatus());
        }
        // Optional: Add check if already refunded

        try {
            if (payment.getPaymentMethod() == Payment.PaymentMethod.STRIPE) {
                // --- Stripe Refund ---
                // Need PaymentIntent ID stored during success/initiation
                String paymentIntentId = payment.getGatewayTransactionId(); // Assumes PI ID stored here
                 if (paymentIntentId == null || !paymentIntentId.startsWith("pi_")) {
                     throw new RefundException("Cannot initiate Stripe refund: PaymentIntent ID not found or invalid on payment record.");
                 }

                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        .build();
                Refund refund = Refund.create(params);
                log.info("Stripe refund created: id={}, status={}", refund.getId(), refund.getStatus());

                // Update Payment status
                payment.setStatus(Payment.PaymentStatus.REFUNDED); // Or REFUND_PENDING if async
                payment.setRefundTransactionId(refund.getId()); // Add this field to Payment entity
                paymentRepository.save(payment);

            } else if (payment.getPaymentMethod() == Payment.PaymentMethod.PAYPAL) {
                // --- PayPal Refund ---
                 log.warn("PayPal refund integration not fully implemented.");
                 // 1. Need the PayPal Capture ID (usually obtained after successful payment capture webhook)
                 //    This might need to be stored on the Payment entity (e.g., gatewayCaptureId)
                 // String captureId = payment.getGatewayCaptureId();
                 // if (captureId == null) { throw new RefundException("PayPal Capture ID not found."); }
                 // 2. Build PayPal RefundRequest object (amount optional for full refund)
                 // 3. Use PayPalHttpClient to execute CapturesRefundRequest(captureId, refundRequest)
                 // 4. Update Payment status (REFUNDED) and store refund ID

            } else {
                 throw new RefundException("Unsupported payment method for refund: " + payment.getPaymentMethod());
            }
        } catch (Exception e) {
            log.error("Refund initiation failed for paymentId: {}", paymentId, e);
            throw new RefundException("Failed to initiate refund: " + e.getMessage(), e);
        }
    }
}