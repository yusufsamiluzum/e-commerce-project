package com.ecommerce.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoComparison {
    private Long comparisonId;
    private String name;
    private Long categoryId; // Category being compared
    private String categoryName;
    private List<DtoProduct> products; // Detailed product DTOs for comparison view
    private LocalDateTime createdAt;
}
