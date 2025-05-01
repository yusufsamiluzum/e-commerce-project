package com.ecommerce.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.ecommerce.dto.DtoAddress;
import com.ecommerce.entities.user.Address;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    // Map relevant fields from Address entity to DtoAddress
    DtoAddress toDto(Address address);
    List<DtoAddress> toDtoList(List<Address> addresses);
}