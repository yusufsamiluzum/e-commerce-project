package com.ecommerce.mappers;

import com.ecommerce.dto.DtoProduct; // Detailed Product DTO
import com.ecommerce.dto.DtoProductSummary; // Summary Product DTO
import com.ecommerce.entities.product.Product; // Product Entity
import org.mapstruct.Mapper; // Optional: If using MapStruct
import org.mapstruct.Mapping; // Optional: If using MapStruct

/**
 * Interface for mapping Product entities to various Product DTOs.
 * This is typically implemented using a mapping framework like MapStruct.
 */
@Mapper(componentModel = "spring") // Optional: Configure for Spring DI if using MapStruct
public interface ComparisonMapper {

    /**
     * Maps a Product entity to its detailed DTO representation (DtoProduct).
     * Explicitly ignores properties not intended for direct mapping in this context.
     *
     * @param product The Product entity to map.
     * @return The corresponding DtoProduct.
     */
    @Mapping(target = "attributes", ignore = true)      // Ignore 'attributes' as it's not directly in Product or needs custom mapping
    @Mapping(target = "images", ignore = true)          // Ignore 'images' as it's not directly in Product or needs custom mapping
    @Mapping(target = "variants", ignore = true)        // Ignore 'variants' as it's not directly in Product or needs custom mapping
    
    DtoProduct productToDtoProduct(Product product);

    /**
     * Maps a Product entity to its summary DTO representation (DtoProductSummary).
     * Explicitly ignores properties not intended for direct mapping in this context.
     *
     * @param product The Product entity to map.
     * @return The corresponding DtoProductSummary.
     */
    @Mapping(target = "primaryImageUrl", ignore = true) // Ignore 'primaryImageUrl' as it's not directly in Product (might need custom logic to derive)
    // Removed ignore for attributes, images, variants as they are not fields in DtoProductSummary
    DtoProductSummary productToDtoProductSummary(Product product);

    // You could add other mapping methods here if needed, e.g.,
    // DtoComparison productComparisonToDto(ProductComparison comparison);
}
