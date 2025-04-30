package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoAddress {

    private Long addressId;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private boolean isDefault;
    private boolean isBilling;
    private boolean isShipping;
    // Timestamps might be omitted unless needed in the UI
    // private LocalDateTime createdAt;
    // private LocalDateTime updatedAt;
    
}

