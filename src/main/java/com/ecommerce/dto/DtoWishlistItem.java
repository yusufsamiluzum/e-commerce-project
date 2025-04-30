package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// --- Wishlist DTOs ---
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoWishlistItem { // Often just the product summary is needed
    private DtoProductSummary product;
    // Maybe add date added?
    // private LocalDateTime dateAdded;
}
