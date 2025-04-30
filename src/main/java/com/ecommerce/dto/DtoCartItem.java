package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoCartItem {
    private Long cartItemId;
    private int quantity;
    private DtoProductSummary product; // Embed product summary
    // Optionally add calculated line item total price
}
