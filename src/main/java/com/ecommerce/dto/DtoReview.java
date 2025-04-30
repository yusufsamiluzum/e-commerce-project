package com.ecommerce.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoReview {
    private Long reviewId;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
    private DtoUserSummary customer; // Show who wrote the review
    private Long productId; // Reference back to product
}
