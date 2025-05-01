package com.ecommerce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.product.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "productId", target = "productId")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "averageRating", target = "averageRating")
    @Mapping(source = "brand", target = "brand")
    @Mapping(source = "model", target = "model")
    // Explicitly ignore primaryImageUrl as there's no direct source in Product
    @Mapping(target = "primaryImageUrl", ignore = true)
    DtoProductSummary toDtoSummary(Product product);
}