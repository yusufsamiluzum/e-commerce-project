package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ecommerce.entities.order.Order.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoOrder { // For order history and detail view
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DtoAddress shippingAddress;
    private DtoAddress billingAddress;
    private List<DtoOrderItem> items;
    private DtoPayment payment; // Embed payment summary
    private List<DtoShipment> shipments; // Embed shipment summaries
    private DtoUserSummary customer; // Optional, usually implicit in customer context
    // private DtoUserSummary seller; // If orders can span multiple sellers, otherwise redundant
}
