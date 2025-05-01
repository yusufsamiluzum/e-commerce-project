package com.ecommerce.services;

import java.util.List;

import com.ecommerce.dto.DtoOrderRequest;
import com.ecommerce.dto.DtoOrderResponse;
import com.ecommerce.entities.order.Order.OrderStatus;

public interface OrderService {

    // Takes DtoOrderRequest, requires authenticated customerId
    DtoOrderResponse createOrder(DtoOrderRequest orderRequestDTO, Long customerId); // Added customerId param

    // Returns DtoOrderResponse
    DtoOrderResponse getOrderById(Long orderId, Long userId, String userRole);

    // Returns List<DtoOrderResponse>
    List<DtoOrderResponse> getOrdersByCustomerId(Long customerId);

    // Returns List<DtoOrderResponse>
    List<DtoOrderResponse> getAllOrders(/* Add Pagination Params */);

    // Returns DtoOrderResponse
    DtoOrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus, Long userId);

    // Returns DtoOrderResponse
    DtoOrderResponse cancelOrder(Long orderId, Long userId, String userRole);

    // No longer public needed if using Mappers
    // DtoOrderResponse convertToDto(Order order);
}
