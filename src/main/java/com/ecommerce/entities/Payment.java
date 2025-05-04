package com.ecommerce.entities;

import java.math.BigDecimal;

import com.ecommerce.entities.order.Order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    private String gatewayTransactionId;
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "refund_transaction_id", unique = true) // Optional: map to specific column, make unique
    private String refundTransactionId; 
    
    // Getters & Setters
    public enum PaymentMethod { STRIPE, PAYPAL }
    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        SUCCESS, // Or COMPLETED, AUTHORIZED, etc.
        FAILED,    // <-- The likely missing value (maybe named FAILED instead of PAYMENT_FAILED)
        REFUNDED,
        CANCELLED
        // Add other relevant statuses
    }
}
