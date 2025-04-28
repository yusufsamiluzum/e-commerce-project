package com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoProduct {

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
    private Set<String> keyFeatures = new HashSet<>();
    private Map<String, String> specifications = new HashMap<>();
    private Double averageRating;
    private Integer reviewCount;

}
