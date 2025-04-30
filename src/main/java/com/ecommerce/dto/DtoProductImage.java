package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoProductImage {
    private Long imageId;
    private String imageUrl;
    private boolean isPrimary;
    private String altText;
}
