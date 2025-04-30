package com.ecommerce.dto;

import java.math.BigDecimal;

import com.ecommerce.entities.Payment.PaymentMethod;
import com.ecommerce.entities.Payment.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoPayment { // Simplified Payment info for Order DTO
    private Long paymentId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private BigDecimal amount;
    private String transactionId; // May be sensitive, consider if needed
}