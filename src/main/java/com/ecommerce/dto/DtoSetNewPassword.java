package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoSetNewPassword { // Input DTO for password change
    private String oldPassword;
    private String newPassword;
}
