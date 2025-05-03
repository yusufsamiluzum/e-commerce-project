package com.ecommerce.dto;

import java.util.Date;

import com.ecommerce.entities.user.User.Sex;
import com.ecommerce.entities.user.User.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoProfile {
    // Note: userId is usually obtained from the security context/path, not part of the body DTO
    private String username; // Often read-only after creation
    private String email;
    private String firstName;
    private String lastName;
    private Sex sex;
    private String phoneNumber;
    private Date dateOfBirth; // Consider using LocalDate if possible, but sticking to original Date
    private UserStatus status;
    // Addresses are handled via separate endpoints/methods in ProfileService v2
    // private List<DtoAddress> addresses = new ArrayList<>(); // Removed as per ProfileService v2 design
}
