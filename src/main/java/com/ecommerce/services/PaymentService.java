package com.ecommerce.services;

import com.ecommerce.dto.DtoPaymentInitiationResponse; // Create this DTO
import com.ecommerce.entities.Payment;

public interface PaymentService {

    /**
     * Initiates the payment process for a given order and payment method.
     * Communicates with the selected payment gateway to set up the transaction.
     *
     * @param orderId The ID of the order to pay for.
     * @param paymentMethod The chosen payment method (STRIPE or PAYPAL).
     * @param customerId The ID of the customer making the payment (for authorization).
     * @return DTO containing necessary info for the frontend (e.g., Stripe client secret, PayPal order ID).
     * @throws com.ecommerce.exceptions.OrderNotFoundException if order not found.
     * @throws com.ecommerce.exceptions.PaymentException if payment initiation fails.
     * @throws com.ecommerce.exceptions.UnauthorizedAccessException if user cannot access the order/payment.
     */
    DtoPaymentInitiationResponse initiatePayment(Long orderId, Payment.PaymentMethod paymentMethod, Long customerId);

    /**
     * Handles incoming webhook notifications from Stripe.
     * Verifies the webhook signature and updates payment/order status.
     *
     * @param payload The raw request body payload from Stripe.
     * @param signatureHeader The value of the 'Stripe-Signature' header.
     * @throws com.ecommerce.exceptions.WebhookVerificationException if signature is invalid.
     * @throws com.ecommerce.exceptions.PaymentException for processing errors.
     */
    void handleStripeWebhook(String payload, String signatureHeader);

    /**
     * Handles incoming webhook notifications from PayPal.
     * Verifies the webhook signature and updates payment/order status.
     *
     * @param payload The raw request body payload from PayPal.
     * @param headers All request headers from PayPal (needed for verification).
     * @throws com.ecommerce.exceptions.WebhookVerificationException if signature is invalid.
     * @throws com.ecommerce.exceptions.PaymentException for processing errors.
     */
    void handlePayPalWebhook(String payload, java.util.Map<String, String> headers);


    /**
     * Initiates a refund for a previously successful payment.
     *
     * @param paymentId The ID of the payment to refund.
     * @throws com.ecommerce.exceptions.PaymentNotFoundException if payment not found.
     * @throws com.ecommerce.exceptions.RefundException if refund is not possible or fails.
     */
    void initiateRefund(Long paymentId);

    // Optional: Method to get payment details if needed separately
    // DtoPayment getPaymentDetails(Long paymentId, Long userId, String userRole);
}
