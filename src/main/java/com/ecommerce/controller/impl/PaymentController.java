package com.ecommerce.controller.impl;


import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.dto.DtoPaymentInitiationRequest; // Create this simple DTO
import com.ecommerce.dto.DtoPaymentInitiationResponse;
import com.ecommerce.entities.Payment; // For PaymentMethod enum
import com.ecommerce.services.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // For getting user ID
import org.springframework.security.core.userdetails.UserDetails;        // For getting user ID (adjust based on your auth)
import org.springframework.web.bind.annotation.*;


import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // Endpoint for frontend to initiate payment after order creation
    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<DtoPaymentInitiationResponse> initiatePayment(
            @PathVariable Long orderId,
            @RequestBody DtoPaymentInitiationRequest request, // Contains PaymentMethod
            @AuthenticationPrincipal UserDetails userDetails) { // Adapt to your auth principal

        // --- Get customer ID from authenticated principal ---
        // This depends heavily on your Spring Security setup. Example:
        Long customerId = getCustomerIdFromPrincipal(userDetails);

        DtoPaymentInitiationResponse response = paymentService.initiatePayment(orderId, request.getPaymentMethod(), customerId);
        return ResponseEntity.ok(response);
    }

    // --- Stripe Webhook Endpoint ---
    @PostMapping("/webhook/stripe")
    @ResponseStatus(HttpStatus.OK) // Respond 200 OK quickly to Stripe
    public void handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        log.info("Received Stripe webhook");
        try {
            paymentService.handleStripeWebhook(payload, sigHeader);
        } catch (Exception e) {
            // Log error but still return 2xx to Stripe if possible, unless verification failed badly
             log.error("Error handling Stripe webhook, but responding OK to avoid retry storms unless critical: {}", e.getMessage());
             // Depending on exception type, consider returning 400 or 500
             // if (e instanceof WebhookVerificationException) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        // No explicit return needed due to @ResponseStatus(HttpStatus.OK)
    }

     // --- PayPal Webhook Endpoint ---
     @PostMapping("/webhook/paypal")
     @ResponseStatus(HttpStatus.OK)
     public void handlePayPalWebhook(@RequestBody String payload, @RequestHeader Map<String, String> headers) {
         log.info("Received PayPal webhook");
         // Convert header names to lowercase for consistency if needed by SDK
         Map<String, String> lowercaseHeaders = headers.entrySet().stream()
             .collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue));
         try {
             paymentService.handlePayPalWebhook(payload, lowercaseHeaders);
         } catch (Exception e) {
             log.error("Error handling PayPal webhook: {}", e.getMessage());
             // Decide on response code based on error type
         }
     }


    // --- Helper to get customer ID (IMPLEMENT BASED ON YOUR AUTH) ---
     private Long getCustomerIdFromPrincipal(UserDetails userDetails) {
         if (userDetails instanceof UserPrincipal) {
            // 1. Cast userDetails to your UserPrincipal class
            // 2. Call getUser() to get the User object
            // 3. Call getUserId() on the User object
            return ((UserPrincipal) userDetails).getUser().getUserId(); // Corrected line
         }
        // Fallback or throw error if user details are not as expected
        throw new RuntimeException("Cannot extract customer ID from principal: Unexpected principal type.");
    }
}


