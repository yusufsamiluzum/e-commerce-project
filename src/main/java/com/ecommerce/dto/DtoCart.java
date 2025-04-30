package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoCart {
    private Long cartId;
    private List<DtoCartItem> items;
    private BigDecimal calculatedTotal; // Calculate in service layer
}
