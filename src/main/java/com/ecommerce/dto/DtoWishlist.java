package com.ecommerce.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoWishlist {
    private Long wishlistId;
    private List<DtoWishlistItem> items; // Or List<DtoProductSummary>
}

