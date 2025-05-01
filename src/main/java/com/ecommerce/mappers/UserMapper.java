package com.ecommerce.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.entities.user.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
     // Map relevant fields from Customer/User entity to DtoUserSummary
     @Mapping(source="userId", target="userId")
     @Mapping(source="username", target="username")
     @Mapping(source="firstName", target="firstName")
     @Mapping(source="lastName", target="lastName")
     DtoUserSummary toDtoSummary(User user); // Can accept User, Customer, Seller etc.
}
