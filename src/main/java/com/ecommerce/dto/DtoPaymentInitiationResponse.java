package com.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

import com.ecommerce.entities.Payment.PaymentMethod;

@Data
@Builder
public class DtoPaymentInitiationResponse {
    private Long paymentId;
    private PaymentMethod paymentMethod;
    private String clientSecret; // For Stripe
    private String paypalOrderId; // For PayPal
    private BigDecimal amount;
    private String currency; // e.g., "USD"
}
