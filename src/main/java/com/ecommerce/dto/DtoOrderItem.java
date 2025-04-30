package com.ecommerce.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// --- Order DTOs ---
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoOrderItem {
    private Long orderItemId;
    private int quantity;
    private BigDecimal priceAtPurchase;
    private DtoProductSummary product; // Embed product summary
}
