package com.ecommerce.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DtoOrderRequest {
    @NotNull(message = "Shipping address ID cannot be null")
    private Long shippingAddressId;

    @NotNull(message = "Billing address ID cannot be null")
    private Long billingAddressId;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<DtoOrderItemRequest> items;

    // Payment details might be handled separately by a Payment service flow
    // private String paymentMethodNonce; // Example: Braintree/PayPal nonce
}
