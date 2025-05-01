package com.ecommerce.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.ecommerce.dto.DtoOrderResponse;
import com.ecommerce.entities.order.Order;



@Mapper(componentModel = "spring", uses = {UserMapper.class, AddressMapper.class, OrderItemMapper.class, PaymentMapper.class, ShipmentMapper.class})
public interface OrderMapper {
    // MapStruct automatically handles lists if element mapping is defined
    DtoOrderResponse toDtoOrderResponse(Order order);
    List<DtoOrderResponse> toDtoOrderResponseList(List<Order> orders);

    // Mapping OrderStatus is direct if names match
    // Mapping totalAmount, createdAt, updatedAt etc. are direct

    // Note: Mappings for nested objects are handled by the 'uses' attribute
    // Example: It will use AddressMapper to map order.shippingAddress to dto.shippingAddress
}

