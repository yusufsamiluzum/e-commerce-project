package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserSummary { // For embedding minimal user info
    private Long userId;
    private String username;
    private String firstName; // Optional: depending on where it's used
    private String lastName; // Optional: depending on where it's used
    private String role;
    private String status;
}
