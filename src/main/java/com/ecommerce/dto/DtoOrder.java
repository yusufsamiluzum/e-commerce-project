package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoOrder {

    private Long orderId;
    private String customerName;
    private String sellerName;
    private String shippingAddress;
    private String paymentMethod;
    private String shipmentStatus;
    private String orderStatus;
    private BigDecimal totalAmount;
    private LocalDateTime orderDateTime;
    private LocalDateTime lastUpdatedDateTime;
}
