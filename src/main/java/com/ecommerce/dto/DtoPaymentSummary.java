package com.ecommerce.dto;

import java.math.BigDecimal;

import com.ecommerce.entities.Payment.PaymentMethod;
import com.ecommerce.entities.Payment.PaymentStatus;

import lombok.Data;

@Data
public class DtoPaymentSummary {
    private Long paymentId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String transactionId; // May be null if pending/failed initially
}
