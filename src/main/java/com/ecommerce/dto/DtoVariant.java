package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoVariant {
    private Long variantId;
    private String sku;
    private BigDecimal priceAdjustment; // Or calculate final price in service?
    private int stockQuantity;
    private List<DtoAttribute> attributes;
}
