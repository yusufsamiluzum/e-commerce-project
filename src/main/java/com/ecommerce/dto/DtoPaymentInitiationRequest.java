package com.ecommerce.dto;


import com.ecommerce.entities.Payment;
import lombok.Data;
@Data
public class DtoPaymentInitiationRequest {
    private Payment.PaymentMethod paymentMethod;
}