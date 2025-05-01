package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok: Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor // Lombok: Generates no-args constructor
@AllArgsConstructor // Lombok: Generates all-args constructor
public class DtoOrderItemRequest {

    /**
     * The unique identifier of the product being ordered.
     * Must not be null.
     */
    @NotNull(message = "Product ID cannot be null")
    private Long productId;

    /**
     * The number of units of the product being ordered.
     * Must be at least 1.
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}