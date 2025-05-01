package com.ecommerce.mappers;

import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.dto.DtoWishlist;
import com.ecommerce.dto.DtoWishlistItem;
import com.ecommerce.entities.Wishlist;
import com.ecommerce.entities.product.Product;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper class to convert between Wishlist entities and DTOs.
 */
@Component // Make it a Spring managed bean
public class WishlistMapper {

    /**
     * Converts a Product entity to a DtoProductSummary.
     * Note: This might already exist in a ProductMapper, reuse if possible.
     * Assuming primaryImageUrl needs to be fetched or constructed separately.
     *
     * @param product The Product entity.
     * @return The corresponding DtoProductSummary.
     */
    public DtoProductSummary toProductSummaryDto(Product product) {
        if (product == null) {
            return null;
        }
        // TODO: Implement logic to get the primary image URL for the product
        // For now, setting it to null or a placeholder.
        String primaryImageUrl = null; // Placeholder

        return new DtoProductSummary(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                primaryImageUrl, // Replace with actual image URL logic
                product.getAverageRating(),
                product.getBrand(),
                product.getModel()
        );
    }

    /**
     * Converts a Product entity to a DtoWishlistItem.
     *
     * @param product The Product entity.
     * @return The corresponding DtoWishlistItem.
     */
    public DtoWishlistItem toWishlistItemDto(Product product) {
        if (product == null) {
            return null;
        }
        return new DtoWishlistItem(toProductSummaryDto(product));
        // If DtoWishlistItem had more fields (like dateAdded), map them here.
    }

    /**
     * Converts a Wishlist entity to a DtoWishlist.
     *
     * @param wishlist The Wishlist entity.
     * @return The corresponding DtoWishlist.
     */
    public DtoWishlist toWishlistDto(Wishlist wishlist) {
        if (wishlist == null) {
            return null;
        }
        DtoWishlist dto = new DtoWishlist();
        dto.setWishlistId(wishlist.getWishlistId());
        dto.setItems(
                wishlist.getProducts().stream()
                        .map(this::toWishlistItemDto) // Use the item mapper
                        .collect(Collectors.toList())
        );
        return dto;
    }

    // No methods needed to map DTOs back to Entities for basic wishlist operations,
    // as we primarily fetch and modify existing entities.
}
