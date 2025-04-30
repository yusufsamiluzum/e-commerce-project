package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoProduct { // For product detail view
    private Long productId;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private String brand;
    private String model;
    private String dimensions;
    private String weight;
    private String color;
    private String warranty;
    private Set<String> keyFeatures;
    private Map<String, String> specifications;
    private Double averageRating;
    private Integer reviewCount;
    private Set<DtoCategory> categories;
    private List<DtoProductImage> images;
    private List<DtoVariant> variants; // Include variants if applicable
    private List<DtoAttribute> attributes; // General product attributes if not variant-specific
    private DtoUserSummary seller; // Summary of the seller
    // Maybe add list of DtoReview summaries?
}
