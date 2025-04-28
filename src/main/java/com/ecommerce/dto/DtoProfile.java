package com.ecommerce.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ecommerce.entities.user.User.Sex;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoProfile {

    
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Sex sex;
    private String phoneNumber;
    private Date dateOfBirth;
    private List<DtoAddress> addresses = new ArrayList<>();
}
