package com.ecommerce.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoProductSummary { // For lists (Cart, Order, Wishlist, Comparison)
    private Long productId;
    private String name;
    private BigDecimal price;
    private String primaryImageUrl; // Simplified from ProductImage
    private Double averageRating;
    private String brand; // Often useful in summaries
    private String model; // Often useful in summaries
}
