package com.ecommerce.mappers;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.dto.DtoOrderItem;
import com.ecommerce.entities.order.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
     @Mapping(source = "product", target = "product") // Use ProductMapper for this
     @Mapping(source = "orderItemId", target = "orderItemId")
     @Mapping(source = "quantity", target = "quantity")
     @Mapping(source = "priceAtPurchase", target = "priceAtPurchase")
     DtoOrderItem toDto(OrderItem orderItem);

     // MapStruct handles the list mapping automatically
     List<DtoOrderItem> toDtoList(List<OrderItem> items);
}
